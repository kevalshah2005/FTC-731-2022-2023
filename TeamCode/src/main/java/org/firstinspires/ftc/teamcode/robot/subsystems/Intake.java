package org.firstinspires.ftc.teamcode.robot.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.robot.hardware.ProfiledServo;
import org.firstinspires.ftc.teamcode.robot.hardware.ProfiledServoPair;
import org.firstinspires.ftc.teamcode.utils.MotionConstraint;

@Config
public class Intake {
    // Config parameters
    public static double P = 0.001;
    public static double clawOpenPos = 0;
    public static double clawClosedPos = 0.7;
    public static double v4bRetractedPos = 0.13;
    public static int intakePartialRetract = 60;
    public static int maxExtension = 835;
    public static int errorTolerance = 20;
    public static double[] stackPositions = {
            0.55,
            0.6,
            0.65,
            0.7,
            0.8
    };

    public final Telemetry telemetry;

    public DcMotorEx slide1;
    public DcMotorEx slide2;

    public int error1, error2;
    public int customTarget;

    public RevColorSensorV3 color;

    public ProfiledServo claw;
    public ProfiledServoPair v4b;

    public DigitalChannel beamBreaker;

    public final ElapsedTime eTime = new ElapsedTime(ElapsedTime.Resolution.SECONDS);

    private enum SlideState {
        RETRACTFULL,
        RETRACTPART,
        EXTENDING,
        CUSTOMEXTEND,
        STOP
    }

    private SlideState slideState = SlideState.RETRACTFULL;

    public Intake(HardwareMap hardwareMap, Telemetry multipleTelemetry) {
        telemetry = multipleTelemetry;

        slide1 = hardwareMap.get(DcMotorEx.class, "intake1");
        slide2 = hardwareMap.get(DcMotorEx.class, "intake2");
        v4b = new ProfiledServoPair(
                hardwareMap,
                "v4b1",
                "v4b2",
                new MotionConstraint(2, 6, 6),
                v4bRetractedPos
        );
        claw = new ProfiledServo(
                hardwareMap,
                "claw",
                new MotionConstraint(1, 6, 6),
                clawClosedPos
        );
        beamBreaker = hardwareMap.get(DigitalChannel.class, "beamBreaker");
        color = hardwareMap.get(RevColorSensorV3.class, "color");

        slide1.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        slide1.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        slide1.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        slide2.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        slide2.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        slide2.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);

        slide2.setDirection(DcMotorSimple.Direction.REVERSE);

        beamBreaker.setMode(DigitalChannel.Mode.INPUT);
    }

    public void update() {
        v4b.periodic();
        claw.periodic();

        switch (slideState) {
            case RETRACTFULL:
                error1 = -slide1.getCurrentPosition();
                error2 = -slide2.getCurrentPosition();

                if (Math.abs(error1) < errorTolerance) {
                    slideState = SlideState.STOP;
                }
                break;
            case RETRACTPART:
                error1 = intakePartialRetract - slide1.getCurrentPosition();
                error2 = intakePartialRetract - slide2.getCurrentPosition();

                if (Math.abs(error1) < errorTolerance) {
                    slideState = SlideState.STOP;
                }
                break;
            case EXTENDING:
                error1 = maxExtension - slide1.getCurrentPosition();
                error2 = maxExtension - slide2.getCurrentPosition();

                if (Math.abs(error1) < errorTolerance) {
                    slideState = SlideState.STOP;
                }
                break;
            case CUSTOMEXTEND:
                error1 = customTarget - slide1.getCurrentPosition();
                error2 = customTarget - slide2.getCurrentPosition();

                if (Math.abs(error1) < errorTolerance) {
                    slideState = SlideState.STOP;
                }
                break;
            case STOP:
                error1 = 0;
                error2 = 0;
                break;
        }

        slide1.setPower(P * error1);
        slide2.setPower(P * error2);
    }

    public void grab() {
        claw.setPosition(clawClosedPos);
    }

    public void release() {
        claw.setPosition(clawOpenPos);
    }

    public boolean isClawBusy() { return claw.isBusy(); };

    public void extendTicks(int ticks, double power, int cycle) {
        customTarget = ticks;
        slideState = SlideState.CUSTOMEXTEND;
        v4b.setPosition(stackPositions[cycle]);
    }

    public void setV4bPos(double pos) {
        v4b.setPosition(pos);
    }

    public boolean isV4BBusy() { return v4b.isBusy(); }

    public int getSlidePosition() {
        return slide1.getCurrentPosition();
    }

    public boolean isBusy() {
        return slideState != SlideState.STOP;
    }

    public void extendFully() {
        slideState = SlideState.EXTENDING;
    }

    public void retractFully() {
        v4b.setPosition(v4bRetractedPos);
        slideState = SlideState.RETRACTFULL;
    }

    public void retractFully(double v4bpos) {
        v4b.setPosition(v4bpos);
        claw.setPosition(clawClosedPos);
        slideState = SlideState.RETRACTFULL;
    }

    public void retractPart(double v4bpos) {
        v4b.setPosition(v4bpos);
        claw.setPosition(clawClosedPos);
        slideState = SlideState.RETRACTPART;
    }

    public void stopSlides() {
        slideState = SlideState.STOP;
    }

    public boolean isConeDetected() {
        return color.getDistance(DistanceUnit.CM) < 2;
    }

    public double[] getMotorPowers() {
        return new double[]{slide1.getPower(), slide2.getPower()};
    }
}
