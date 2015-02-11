package com.sdp.strategy;

import com.sdp.planner.RobotCommands;
import com.sdp.planner.RobotPlanner;
import com.sdp.world.DynamicWorldState;
import com.sdp.world.DynamicWorldState.Ball;
import com.sdp.world.DynamicWorldState.Robot;
import com.sdp.world.SimpleWorldState;
import com.sdp.world.SimpleWorldState.Operation;
import com.sdp.world.WorldState;

public class AttackerStrategy extends GeneralStrategy {
	private final int allowedDegreeError = 15;
	
	public void sendWorldState(DynamicWorldState dynWorldState,
			WorldState worldState) {
		// Initialise robot and ball objects
		Robot robot = dynWorldState.getAttacker();
		Ball ball = dynWorldState.getBall();

		double robotX = robot.getCenter().getX();
		double robotY = robot.getCenter().getY();
		double robotAngleRad = robot.getHeading();
		double robotAngleDeg = Math.toDegrees(robotAngleRad);
		double ballX = ball.getPoint().getX();
		double ballY = ball.getPoint().getY();
		// Desired angle to face ball
		double ballAngleDeg = RobotPlanner.desiredAngle(robotX, robotY,
				robotAngleRad, ballX, ballY);
		double ballDiffInHeadings = Math.abs(robotAngleDeg - ballAngleDeg);
		// Robot is facing the ball if within this angle in degrees of the ball
		boolean isRobotFacingGoal = (ballDiffInHeadings < allowedDegreeError || ballDiffInHeadings > 360 - allowedDegreeError);

		// 1 - Rotate to face ball
		if (!RobotPlanner.doesOurRobotHaveBall(robot, ball)
				&& !isRobotFacingGoal) {
			rotateToDesiredAngle(robotAngleDeg, ballAngleDeg);
			System.out.println("Rotating to face ball.");
		}

		// 2 - Go towards ball if it is in our attacker zone
		System.out.println("Ball is in zone " + inZone(ballX));
		if(worldState.weAreShootingRight){
			if (!RobotPlanner.doesOurRobotHaveBall(robot, ball)
					&& isRobotFacingGoal 
					&& !RobotPlanner.canCatchBall(robot, ball)
					&& (inZone(ballX) == 2)) {
				RobotCommands.goStraight();
				SimpleWorldState.previousOperation = Operation.NONE;
				System.out.println("Moving towards ball.");
			}
		} else {
			if (!RobotPlanner.doesOurRobotHaveBall(robot, ball)
					&& isRobotFacingGoal 
					&& !RobotPlanner.canCatchBall(robot, ball)
					&& (inZone(ballX)==1)) {
				RobotCommands.goStraight();
				SimpleWorldState.previousOperation = Operation.NONE;
				System.out.println("Moving towards ball.");
			}
		}
		

		// 3 - Catch ball
		if (!RobotPlanner.doesOurRobotHaveBall(robot, ball)
				&& isRobotFacingGoal && RobotPlanner.canCatchBall(robot, ball)
				&& !(SimpleWorldState.previousOperation == Operation.CATCH)) {
			RobotCommands.catchBall();
			SimpleWorldState.previousOperation = Operation.CATCH;
			System.out.println("Catching ball.");
		}

		// 4 - Face goal and kick ball (hopefully into the goal!)
		if (RobotPlanner.doesOurRobotHaveBall(robot, ball)) {
			scoreGoal(dynWorldState, worldState);
			System.out.println("Scoring goal!");
		}
	}

	private void rotateToDesiredAngle(double robotAngleDeg,
			double desiredAngleDeg) {
		double diffInHeadings = Math.abs(robotAngleDeg - desiredAngleDeg);
		System.out.println("Difference in headings: " + diffInHeadings);
		if ((diffInHeadings < allowedDegreeError)
				|| (diffInHeadings > 360 - allowedDegreeError)) {

			System.out.println("Desired angle");
			// stopping rotation but not other operations
			if (SimpleWorldState.previousOperation == Operation.RIGHT
					|| SimpleWorldState.previousOperation == Operation.LEFT) {
				RobotCommands.stop();
				SimpleWorldState.previousOperation = Operation.NONE;
			}
		} else {
			System.out.println("Current robot heading:" + robotAngleDeg);
			System.out.println("Angle to face:" + desiredAngleDeg);

			if ((diffInHeadings < allowedDegreeError * 2)
					|| (diffInHeadings > 360 - allowedDegreeError * 2)) {
				RobotCommands.stop();
				boolean shouldRotateRight = RobotPlanner.shouldRotateRight(
						desiredAngleDeg, robotAngleDeg);
				if (shouldRotateRight) {
					RobotCommands.shortRotateRight();
					SimpleWorldState.previousOperation = Operation.SHORT_RIGHT;
				} else if (!shouldRotateRight) {
					RobotCommands.shortRotateLeft();
					SimpleWorldState.previousOperation = Operation.SHORT_LEFT;
				}
				return;
			} else {

				boolean shouldRotateRight = RobotPlanner.shouldRotateRight(
						desiredAngleDeg, robotAngleDeg);
				if (shouldRotateRight
						&& SimpleWorldState.previousOperation != Operation.RIGHT) {
					RobotCommands.rotateRight();
					SimpleWorldState.previousOperation = Operation.NONE;
				} else if (!shouldRotateRight
						&& SimpleWorldState.previousOperation != Operation.LEFT) {
					RobotCommands.rotateLeft();
					SimpleWorldState.previousOperation = Operation.NONE;
				}
				return;
			}
		}
	}

	private void scoreGoal(DynamicWorldState dynWorldState,
			WorldState worldState) {
		// turn towards the goal
		Ball ball = dynWorldState.getBall();
		double ballX = ball.getPoint().getX();
		double ballY = ball.getPoint().getY();
		Robot robot = dynWorldState.getAttacker();
		double robotX = robot.getCenter().getX();
		double robotY = robot.getCenter().getY();
		double robotDir = robot.getHeading();
		double robotAngleDeg = Math.toDegrees(robotDir);
		double rightGoalX = GeneralStrategy.rightGoalX;
		double rightGoalY = GeneralStrategy.rightGoalY;
		double leftGoalX = GeneralStrategy.leftGoalX;
		double leftGoalY = GeneralStrategy.leftGoalY;		
		boolean facingGoal = false;

		System.out.println("goal " + leftGoalX + " " + leftGoalY);
		System.out.println("robot " + robotX + " " + robotY);
		System.out.println("ball " + ballX + " " + ballY);

		double desiredAngleDegb = RobotPlanner.desiredAngle(robotX, robotY,
				robotDir, ballX, ballY);

		System.out.println("desiredAngleBall " + desiredAngleDegb);

		// Decide which goal to aim at, and calculate desired angle
		double desiredAngleDeg = 0.0;
		if(worldState.weAreShootingRight){
			desiredAngleDeg = RobotPlanner.desiredAngle(robotX, robotY,
					robotDir, rightGoalX, rightGoalY);
		} else {
			desiredAngleDeg = RobotPlanner.desiredAngle(robotX, robotY,
					robotDir, leftGoalX, leftGoalY);
		}
		
		System.out.println("desiredAngleGoal " + desiredAngleDeg);
		rotateToDesiredAngle(robotAngleDeg, desiredAngleDeg);
		
		// Decides whether or not the robot is facing the desired goal
		if (Math.abs(robotAngleDeg - desiredAngleDeg) < allowedDegreeError) {
			facingGoal = true;
			System.out.println("Facing goal!");
		} else {
			facingGoal = false;
			System.out.println("Not facing goal.");
		}

		if (SimpleWorldState.previousOperation != Operation.KICK
				&& facingGoal && RobotPlanner.doesOurRobotHaveBall(robot, ball)) { // change to doesRobotHaveBall
			RobotCommands.kick();
			SimpleWorldState.previousOperation = Operation.KICK;
		}
	}
}
