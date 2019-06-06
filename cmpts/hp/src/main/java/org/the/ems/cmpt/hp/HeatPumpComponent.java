/* 
 * Copyright 2016-19 ISC Konstanz
 * 
 * This file is part of TH-E-EMS.
 * For more information visit https://github.com/isc-konstanz/TH-E-EMS
 * 
 * TH-E-EMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TH-E-EMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TH-E-EMS.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.the.ems.cmpt.hp;

import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.cmpt.GeneratorComponent;
import org.the.ems.core.ComponentException;
import org.the.ems.core.EnergyManagementException;
import org.the.ems.core.cmpt.HeatPumpService;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.BooleanValue;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.DoubleValue;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.core.data.WriteContainer;

@Component
public class HeatPumpComponent extends GeneratorComponent implements HeatPumpService {
	private final static Logger logger = LoggerFactory.getLogger(HeatPumpComponent.class);

	private final static String ID = "HeatPump";

	@Configuration("temp_min")
	protected double temperatureMin;

	@Configuration("temp_max")
	protected double temperatureMax;

	@Configuration("temp_in_max")
	protected double temperatureInMax;

	@Configuration("temp_in")
	protected ChannelListener temperature;

	protected Value temperatureValue = DoubleValue.emptyValue();

	@Configuration
	protected ChannelListener state;

	protected Value stateValueLast = null;

	@Override
	public String getId() {
		return ID;
	}

	@Configuration
	protected double cop;

	@Override
	public Value getCoefficientOfPerformance() throws ComponentException {
		return new DoubleValue(cop);
	}

	@Override
	@Configuration("el_energy")
	public Value getElectricalEnergy() throws ComponentException { return getConfiguredValue("el_energy"); }

	@Override
	@Configuration("th_energy")
	public Value getThermalEnergy() throws ComponentException { return getConfiguredValue("th_energy"); }

	@Override
	@Configuration("el_power")
	public Value getElectricalPower() throws ComponentException { return getConfiguredValue("el_power"); }

	@Override
	@Configuration("th_power")
	public Value getThermalPower() throws ComponentException { return getConfiguredValue("th_power"); }

	@Override
	public void onActivate(Configurations configs) throws EnergyManagementException {
		super.onActivate(configs);

		state.registerValueListener(new StateListener());
		temperature.registerValueListener(new TemperatureListener());
	}

	@Override
	public void onDeactivate() throws EnergyManagementException {
		super.onDeactivate();
		
		state.deregister();
		temperature.deregister();
	}

	@Override
	protected void onStart(WriteContainer container, Value value) throws ComponentException {
		if (temperatureValue.doubleValue() > temperatureInMax) {
			throw new ComponentException("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
		}
		container.add(state, new BooleanValue(true, value.getTime()));
	}

	@Override
	protected void onStop(WriteContainer container, Long time) throws ComponentException {
		container.add(state, new BooleanValue(false, time));
	}

	private class StateListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			if (value.booleanValue() && temperatureValue.doubleValue() > temperatureInMax) {
				logger.warn("Unable to switch on heat pump: Heating cycle input temperature above threshold: " + value);
				// TODO: implement virtual start signal that does not affect relay
				state.write(new BooleanValue(false, value.getTime()));
				return;
			}
			else if (value.booleanValue() && stateValueLast != null && !stateValueLast.booleanValue()) {
				startTimeLast = value.getTime();
			}
			stateValueLast = value;
		}
	}

	private class TemperatureListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			temperatureValue = value;
			
			if (temperatureValue.doubleValue() >= temperatureInMax) {
				state.write(new BooleanValue(false, value.getTime()));;
				return;
			}
			if (isMaintenance()) {
				return;
			}
			if (temperatureValue.doubleValue() <= temperatureMin &&
					(stateValueLast != null && !stateValueLast.booleanValue())) {
				
				state.write(new BooleanValue(true, value.getTime()));
			}
			else if (temperatureValue.doubleValue() >= temperatureMax &&
					(stateValueLast != null && stateValueLast.booleanValue())) {
				
				state.write(new BooleanValue(false, value.getTime()));
			}
		}
	}

}
