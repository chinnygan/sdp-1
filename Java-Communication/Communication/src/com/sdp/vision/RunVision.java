package com.sdp.vision;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.UIManager;

import org.apache.commons.cli.Options;

import au.edu.jcu.v4l4j.V4L4JConstants;

import com.sdp.Communication;
import com.sdp.Constants;
import com.sdp.strategy.StrategyController;
import com.sdp.vision.gui.VisionGUI;
import com.sdp.vision.gui.tools.AlignmentTool;
import com.sdp.vision.gui.tools.ColourThresholdConfigTool;
import com.sdp.vision.gui.tools.HistogramTool;
import com.sdp.vision.gui.tools.PitchModelView;
import com.sdp.vision.gui.tools.StrategySelectorTool;
import com.sdp.vision.recognisers.BallRecogniser;
import com.sdp.vision.recognisers.RobotRecogniser;
import com.sdp.world.DynamicWorldState;
import com.sdp.world.Pitch;
import com.sdp.world.WorldState;

/**
 * The main class used to run the vision system. Creates the control GUI, and
 * initialises the image processing.
 */
public class RunVision {
	static Options cmdLineOptions;

	static {
		cmdLineOptions = new Options();
		cmdLineOptions.addOption("nobluetooth", false,
				"Disable Bluetooth support");
	}

	/**
	 * The main method for the class. Creates the control GUI, and initialises
	 * the image processing.
	 * 
	 * @param args
	 *            Program arguments.
	 */
	public static void main(String[] args) {
		Communication.getInstance().initializeSerialPort(Constants.PORT_NAME);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		final YAMLConfig yamlConfig = new YAMLConfig();
		// 0 = default to main pitch
		final PitchConstants pitchConstants = new PitchConstants(0);
		final Pitch pitch = new Pitch(yamlConfig, pitchConstants);
		WorldState worldState = new WorldState(pitch);

		DynamicWorldState dynamicWorldState = new DynamicWorldState();

		// Default values for the main vision window
		String videoDevice = "/dev/video0";
		int width = VideoStream.FRAME_WIDTH;
		int height = VideoStream.FRAME_HEIGHT;
		int channel = 0;
		int videoStandard = V4L4JConstants.STANDARD_PAL;
		int compressionQuality = 100;

		// Create a new Vision object to serve the main vision window
		Vision vision = new Vision(worldState, pitchConstants,
				dynamicWorldState);

		try {
			StrategyController strategyController = null;
			strategyController = new StrategyController();
			Vision.addWorldStateReceiver(strategyController);

			final VideoStream vStream = new VideoStream(videoDevice, width,
					height, channel, videoStandard, compressionQuality);

			DistortionFix distortionFix = new DistortionFix(yamlConfig,
					pitchConstants);

			// Create the Control GUI for threshold setting/etc
			VisionGUI gui = new VisionGUI(width, height, yamlConfig);

			gui.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent e) {
					vStream.shutdown();
				}
			});

			ColourThresholdConfigTool ctct = new ColourThresholdConfigTool(gui,
					worldState, pitchConstants, vStream, distortionFix,
					yamlConfig);
			gui.addTool(ctct, "Settings");
			vision.addRecogniser(ctct.new PitchBoundsDebugDisplay());
			vision.addRecogniser(ctct.new DividerLineDebugDisplay());
			vision.addRecogniser(ctct.new GoalPositionDebugDisplay());

			HistogramTool histogramTool = new HistogramTool(gui, pitchConstants);
			gui.addTool(histogramTool, "Colour Thresholds");

			PitchModelView pmvTool = new PitchModelView(gui, pitchConstants,
					pitch, distortionFix, dynamicWorldState);
			gui.addTool(pmvTool, "Pitch Model");
			pmvTool.addViewProvider(new BallRecogniser.ViewProvider(
					dynamicWorldState, pitch));
			pmvTool.addViewProvider(new RobotRecogniser.ViewProvider(
					dynamicWorldState, pitch));

			vision.addRecogniser(new BallRecogniser(vision, worldState,
					pitchConstants, distortionFix, pitch));
			vision.addRecogniser(new RobotRecogniser(vision, worldState,
					pitchConstants, distortionFix, pitch));

			vision.addRecogniser(histogramTool);

			StrategySelectorTool stratSelect = new StrategySelectorTool(gui,
					strategyController);
			gui.addTool(stratSelect, "Robot and strategy control");

			AlignmentTool alignmentTool = new AlignmentTool(gui);
			gui.addTool(alignmentTool, "Alignment");
			vision.addRecogniser(alignmentTool.new FrameDisplay());

			vStream.addReceiver(pmvTool);
			vStream.addReceiver(distortionFix);
			vStream.addReceiver(vision);
			distortionFix.addReceiver(gui);
			vision.addVisionDebugReceiver(gui);

			gui.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Code for milestone 2
		if (args.length != 0) {
			String action = args[0];
			if (action.equals("attack")) {
				// find the ball, move to this point and catch it
				Constants.currentStrategy = Constants.Strategy.ATTACK;
				System.out.println("Attack strategy was chosen");

			} else if (action.equals("defend")) {
				Constants.currentStrategy = Constants.Strategy.DEFEND;
				System.out.println("Defence strategy was chosen");
			} else {
				System.out.println("Strategy not recognized.");
			}
		} else {
			System.out.println("No strategy specified");
		}
	}
}