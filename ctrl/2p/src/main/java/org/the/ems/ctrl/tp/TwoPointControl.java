package org.the.ems.ctrl.tp;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.the.ems.core.ComponentException;
import org.the.ems.core.config.Configuration;
import org.the.ems.core.config.Configurations;
import org.the.ems.core.data.ChannelListener;
import org.the.ems.core.data.Value;
import org.the.ems.core.data.ValueListener;
import org.the.ems.ctrl.Control;

@Component(
	scope = ServiceScope.BUNDLE,
	configurationPid = Control.PID+".2p",
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class TwoPointControl extends Control {
	private final static Logger logger = LoggerFactory.getLogger(TwoPointControl.class);

	@Configuration("temp_max")
	private double temperatureMax;

	@Configuration("temp_min")
	private double temperatureMin;

	@Configuration("temp")
	private ChannelListener temperature;

	private double temperatureValue = Double.NaN;

	@Override
	public void onActivate(Configurations configs) throws ComponentException {
		super.onActivate(configs);

		temperature.registerValueListener(new TemperatureListener());
	}

	@Override
	public void onDeactivate() throws ComponentException {
		super.onDeactivate();
		
		temperature.deregister();
	}

	protected boolean isMinTemperature() {
		return temperatureValue <= temperatureMin;
	}

	protected boolean isMaxTemperature() {
		return temperatureValue >= temperatureMax;
	}

	private class TemperatureListener implements ValueListener {

		@Override
		public void onValueReceived(Value value) {
			String message = "Received temperature: {}";
			
			temperatureValue = value.doubleValue();
			if (isMinTemperature()) {
				start();
				message += " <= "+temperatureMin+" -> ON";
			}
			else if (isMaxTemperature()) {
				stop();
				message += " >= "+temperatureMax+" -> OFF";
			}
			logger.debug(message, temperatureValue);
		}
	}

}
