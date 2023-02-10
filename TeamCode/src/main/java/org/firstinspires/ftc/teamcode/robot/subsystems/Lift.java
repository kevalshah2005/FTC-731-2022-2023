package org.firstinspires.ftc.teamcode.robot.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.arcrobotics.ftclib.controller.PIDController;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.robot.hardware.ProfiledServo;
import org.firstinspires.ftc.teamcode.robot.hardware.ProfiledServoPair;
import org.firstinspires.ftc.teamcode.utils.MotionConstraint;

@Config
public class Lift {
    // Config parameters
    public static PIDController liftController = new PIDController(0.005, 0, 0);
    public static int liftLow = 900;
    public static int liftMid = 1750;
    public static int liftHigh = 2450;
    public static double grabPos = 0.45;
    public static double releasePos = 0.9;
    public static double waitTime = 1.5;
    public static int hoverPos = 800;
    public static int collectPos = 200;
    public static int minHeightForArmRotation = 200;
    public static int errorTolerance = 10;
    public static double grabTime = 0.75;
    public static double yawArmAngle = -10;
    public static double yawArmRetracted = 0;
    public static double yawArmExtended = 1;

    private final Telemetry telemetry;

    private final DcMotorEx lift1;
    private final DcMotorEx lift2;
    private final ProfiledServoPair yawArm;
    private final Servo grabber;
    private final ProfiledServo yawArmExtension;

    private int targetPosition = 0;
    private int error1 = 0;
    private int error2 = 0;

    private double currentYawArmAngle = 0;

    public enum LiftState {
        HIGH,
        MID,
        LOW,
        RETRACT,
        COLLECT,
        ZERO
    }

    private LiftState liftState = LiftState.RETRACT;

    private enum GrabberState {
        HOLD,
        RELEASE,
        DEPOSITING
    }

    private GrabberState grabberState = GrabberState.HOLD;

    private enum YawArmState {
        RETRACTED,
        EXTENDED
    }

    private YawArmState yawArmState = YawArmState.RETRACTED;

    private final ElapsedTime grabTimer = new ElapsedTime();

    public Lift(HardwareMap hardwareMap, Telemetry multipleTelemetry) {
        telemetry = multipleTelemetry;

        lift1 = hardwareMap.get(DcMotorEx.class, "lift1");
        lift2 = hardwareMap.get(DcMotorEx.class, "lift2");
        yawArm = new ProfiledServoPair(
                hardwareMap,
                "yaw1",
                "yaw2",
                new MotionConstraint(3, 4, 3),
                (0.0037037 * -45) + 0.33333
        );

        grabber = hardwareMap.get(Servo.class, "grab");
        grabber.setPosition(grabPos);

        yawArmExtension = new ProfiledServo(
                hardwareMap,
                "yawArmExtension",
                new MotionConstraint(2, 4, 4),
                yawArmRetracted
        );

        lift1.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        lift1.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        lift1.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        lift2.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        lift2.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        lift2.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
    }

    public void setLiftState(LiftState state) {
        liftState = state;
    }

    public LiftState getLiftState() {
        return liftState;
    }

    public void closeGrabber() { grabberState = GrabberState.HOLD; }

    public void openGrabber() { grabberState = GrabberState.RELEASE; }

    public void setYawArmAngle(double angle) {
        currentYawArmAngle = angle;

        while (angle < -180) {
            angle += 360;
        }
        while (angle > 180) {
            angle -= 360;
        }
        if (angle < -90) {
            if (angle > -135) {
                angle = -90;
            } else {
                angle = -180;
            }
        }
        double pos = (0.0037037 * angle) + 0.33333;
        yawArm.setPosition(pos);
    }
    public double getYawArmAngle() {
        return currentYawArmAngle;
    }

    public int getSlidePosition() { return lift1.getCurrentPosition(); }

    public double[] getMotorPowers() { return new double[]{lift1.getPower(), lift2.getPower()}; }

    public int getTargetPosition() { return targetPosition; }

    public boolean canControlArm() {
        return lift1.getCurrentPosition() > minHeightForArmRotation;
    }

    public boolean isBusy() { return liftController.getPositionError() >= errorTolerance; }

    public boolean isYawArmBusy() { return yawArm.isBusy(); }

    public void update() {
        switch (liftState) {
            case HIGH:
                targetPosition = liftHigh;
                break;
            case MID:
                targetPosition = liftMid;
                break;
            case LOW:
                targetPosition = liftLow;
                break;
            case RETRACT:
                grabber.setPosition(grabPos);
                setYawArmAngle(yawArmAngle);
                if (!isYawArmBusy()) {
                    targetPosition = hoverPos;
                }
                break;
            case COLLECT:
                setYawArmAngle(yawArmAngle);
                targetPosition = collectPos;
                break;
            case ZERO:
                setYawArmAngle(-45);
                targetPosition = 0;
                break;
        }

        lift1.setPower(liftController.calculate(lift1.getCurrentPosition(), targetPosition));
        lift2.setPower(liftController.calculate(lift2.getCurrentPosition(), targetPosition));

        switch (grabberState) {
            case HOLD:
                grabber.setPosition(grabPos);
                break;
            case RELEASE:
                grabber.setPosition(releasePos);
                break;
        }

        switch (yawArmState) {
            case RETRACTED:
                yawArmExtension.setPosition(yawArmRetracted);
                break;

            case EXTENDED:
                yawArmExtension.setPosition(yawArmExtended);
                break;
        }

        yawArm.periodic();
        yawArmExtension.periodic();
    }
}
