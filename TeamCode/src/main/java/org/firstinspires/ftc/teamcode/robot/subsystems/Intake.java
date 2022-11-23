package org.firstinspires.ftc.teamcode.robot.subsystems;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.ServoImplEx;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;

@Config
public class Intake {
    // Config parameters
    public static double clawOpenPos = 0;
    public static double clawClosedPos = 1;
    public static double v4b1RetractedPos = 0;
    public static double v4b1ExtendedPos = 1;
    public static double v4b2RetractedPos = 1;
    public static double v4b2ExtendedPos = 0;
    public static double desiredSlidePower = 0.5;
    public static double maxExtension = 1500;
    public static double grabTime = 1;

    public enum IntakeState {
        RETRACTED,
        EXTENDING,
        EXTENDED,
        GRABBING,
        RETRACTING,
        TRANSFERRING
    }

    public IntakeState intakeState = IntakeState.RETRACTED;

    public final Telemetry telemetry;

    public DcMotorEx slide1;
    public DcMotorEx slide2;

    public ServoImplEx claw;
    public ServoImplEx v4b1;
    public ServoImplEx v4b2;

    public DigitalChannel beamBreaker;

    public final ElapsedTime eTime = new ElapsedTime(ElapsedTime.Resolution.SECONDS);

    public int depositTicks;

    public boolean grabbing = false;
    public boolean previousGrabButton = false;

    public Intake(HardwareMap hardwareMap, Telemetry multipleTelemetry) {
        telemetry = multipleTelemetry;

        slide1 = hardwareMap.get(DcMotorEx.class, "intake1");
        slide2 = hardwareMap.get(DcMotorEx.class, "intake2");
        v4b1 = hardwareMap.get(ServoImplEx.class, "v4b1");
        v4b2 = hardwareMap.get(ServoImplEx.class, "v4b2");
        claw = hardwareMap.get(ServoImplEx.class, "claw");
        beamBreaker = hardwareMap.get(DigitalChannel.class, "beamBreaker");

        slide1.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        slide1.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        slide1.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);
        slide2.setZeroPowerBehavior(DcMotorEx.ZeroPowerBehavior.BRAKE);
        slide2.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);
        slide2.setMode(DcMotorEx.RunMode.RUN_USING_ENCODER);

        v4b1.setPosition(v4b1RetractedPos);
        v4b2.setPosition(v4b2RetractedPos);

        claw.setPosition(clawOpenPos);

        beamBreaker.setMode(DigitalChannel.Mode.INPUT);
    }

    public void intake(boolean intakeExtendButton, boolean grabButton, boolean cancelAutomation) {
        telemetry.addData("Intake State", intakeState);
        telemetry.addData("Intake Encoder Value", slide1.getCurrentPosition());
        telemetry.addData("Intake Target Position", slide1.getTargetPosition());
        telemetry.addData("Claw Servo Pos", claw.getPosition());
        telemetry.addData("Timer", eTime.time());
        telemetry.update();

        switch (intakeState) {
            case RETRACTED:
                slide1.setTargetPosition(0);
                slide2.setTargetPosition(0);
                v4b1.setPosition(v4b1RetractedPos);
                v4b2.setPosition(v4b2RetractedPos);
                claw.setPosition(clawOpenPos);
                if (intakeExtendButton) {
                    slide1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    slide2.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
                    intakeState = IntakeState.EXTENDING;
                }
                break;

            case EXTENDING:
                slide1.setPower(desiredSlidePower);
                slide2.setPower(desiredSlidePower);
                v4b1.setPosition(v4b1ExtendedPos);
                v4b2.setPosition(v4b2ExtendedPos);
                if (slide1.getCurrentPosition() > maxExtension || beamBreaker.getState()) {
                    intakeState = IntakeState.EXTENDED;
                }
                break;

            case EXTENDED:
                slide1.setPower(0);
                slide2.setPower(0);
                if (grabButton) {
                    eTime.reset();
                    intakeState = IntakeState.GRABBING;
                }
                break;

            case GRABBING:
                claw.setPosition(clawClosedPos);
                if (eTime.time() > grabTime) {
                    intakeState = IntakeState.RETRACTING;
                }
                break;

            case RETRACTING:
                slide1.setTargetPosition(0);
                slide2.setTargetPosition(0);
                slide1.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                slide2.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                v4b1.setPosition(v4b1RetractedPos);
                v4b2.setPosition(v4b2RetractedPos);
                if (Math.abs(slide1.getCurrentPosition()) < 20) {
                    intakeState = IntakeState.RETRACTED;
                }
                break;
        }

        if (cancelAutomation) {
            intakeState = IntakeState.RETRACTED;
        }
    }
}