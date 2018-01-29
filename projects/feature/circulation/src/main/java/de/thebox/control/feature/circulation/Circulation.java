package de.thebox.control.feature.circulation;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import de.thebox.control.core.ControlService;
import de.thebox.control.core.component.ComponentConfigException;
import de.thebox.control.core.data.Channel;
import de.thebox.control.core.data.DoubleValue;
import de.thebox.control.core.data.UnknownChannelException;
import de.thebox.control.core.data.Value;
import de.thebox.control.feature.circulation.CirculationTemperatureListener.CirculationTemperatureCallbacks;

public class Circulation implements CirculationTemperatureCallbacks {

	protected final Channel deltaTemperature;
	protected Value outTemperatureLast = null;
	protected Value inTemperatureLast = null;
	protected final List<CirculationTemperatureListener> temperatureListeners = new ArrayList<CirculationTemperatureListener>();

	public Circulation(ControlService control, Preferences prefs) throws ComponentConfigException {
		CirculationConfig config = new CirculationConfig(prefs);
		try {
			this.deltaTemperature = control.getChannel(config.getDeltaTemperature());
			registerTemperatureListener(control.getChannel(config.getInTemperature()), CirculationTemperature.IN);
			registerTemperatureListener(control.getChannel(config.getOutTemperature()), CirculationTemperature.OUT);
			
		} catch (UnknownChannelException e) {
			throw new ComponentConfigException("Invalid circulation configuration: " + e.getMessage());
		}
	}

	protected void registerTemperatureListener(Channel channel, CirculationTemperature type) {
		CirculationTemperatureListener listener = new CirculationTemperatureListener(this, type, channel);
		temperatureListeners.add(listener);
	}

	public void deactivate() {
		for (CirculationTemperatureListener listener: temperatureListeners) {
			listener.deregister();
		}
	}

	@Override
	public synchronized void onTemperatureReceived(CirculationTemperature type, Value temperature) {
		switch(type) {
		case IN:
			inTemperatureLast = temperature;
			break;
		case OUT:
			outTemperatureLast = temperature;
			break;
		case REF:
			onTemperatureReferenceUpdated(temperature);
			break;
		}
		if (type == CirculationTemperature.OUT || type == CirculationTemperature.IN) {
			if (outTemperatureLast != null && inTemperatureLast != null &&
					outTemperatureLast.getTimestamp().equals(inTemperatureLast.getTimestamp())) {
				
				double delta = outTemperatureLast.doubleValue() - inTemperatureLast.doubleValue();
				onTemperatureDeltaUpdated(new DoubleValue(delta, outTemperatureLast.getTimestamp()));
			}
		}
	}

	protected  void onTemperatureReferenceUpdated(Value delta) {
		// Placeholder implementation for the circulation pump control
	}

	protected  void onTemperatureDeltaUpdated(Value delta) {
		deltaTemperature.setLatestValue(delta);
	}
}
