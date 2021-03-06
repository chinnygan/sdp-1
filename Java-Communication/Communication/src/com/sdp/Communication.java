package com.sdp;


import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import jssc.SerialPortTimeoutException;

public class Communication {
	private boolean isPortInitialized = false;
	static Communication instance;

	private Communication() {

	}

	public boolean isPortInitialized() {
		return isPortInitialized;
	}

	public static Communication getInstance() {
		if (instance == null) {
			instance = new Communication();
		}
		return instance;
	}

	private SerialPort serialPort;

	public String[] getAvailablePorts() {
		String[] portNames = SerialPortList.getPortNames();

		return portNames;
	}

	public void initializeSerialPort(String portName) {
		serialPort = new SerialPort(portName);
		isPortInitialized = true;
		try {
			serialPort.openPort();
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	void sendCommandViaPort(String command) {
		try {
			serialPort.writeString(command);
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	public String readStringFromSerialPort() {
		String input = null;
		String lastLine = null;
		try {
			input = serialPort.readString();
			lastLine = input.substring(input.lastIndexOf("\n")-4).replaceAll("[^0-9]+", "");
			System.out.println("String received via serial port " + lastLine);
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
		return lastLine;
	}
	
    
    
    

	public class ReadStringRunnable implements Runnable {
		public ReadStringRunnable() {
			System.out.println("Read String Runnable started ");
		}

		@Override
		public void run() {
			while (true) {
				String input;
				try {
					input = serialPort.readString();
					if (input != null) {
						System.out.println("String received via serial port "
								+ input);
						break;
					}
				} catch (SerialPortException e) {
					e.printStackTrace();
				}
			}
		}

	}
}