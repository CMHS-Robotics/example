package frc.robot.RotationLocker;

import edu.wpi.first.math.controller.PIDController;
import frc.robot.limelightData;



public class AprilTagRotationLock implements RotationSource {

    
    

    public PIDController rotationPID = createPIDController();
    private  PIDController createPIDController() {

        

        PIDController pid = new PIDController(0.0015, 0.0015, 0.00015);
        pid.setTolerance(.25); // allowable angle error
        pid.enableContinuousInput(0, 360); // it is faster to go 1 degree from 359 to 0 instead of 359 degrees
        pid.setSetpoint(0); // 0 = apriltag angle/offset
        return pid;
    }

    @Override
    public double getRd(double degreesOffset) {
        double calculatedValue = rotationPID.calculate(degreesOffset);
        return calculatedValue;
    }

    @Override
    public double getR() {
        throw new UnsupportedOperationException("Unimplemented method 'getR'");
    }


}
