import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

public class DiogoFredRobot extends AdvancedRobot {
	EnemyRobot enemy = new EnemyRobot();
	double xmax = 0;  //guarda o comprimento do campo
	double ymax = 0;  //guarda a largura do campo
	
	byte moveDirection = 1; 
	
	double oldEnemyHeading = 0;
    
    int wallAvoidance = 0; 
    double timeWhenDirectionChanged = 0;
    double wallMargin = 100;

	public void run () {
		xmax = getBattleFieldWidth();
		ymax = getBattleFieldHeight();
		
		setAdjustRadarForGunTurn(true); 
		setAdjustGunForRobotTurn(true); 
		
		turnRadarRightRadians(Double.POSITIVE_INFINITY); 
		
		while(true) {
			scan();
			move();
			fire();
			execute();
		}
	}
	
	/**
	 * When an enemy is scanned the radar is locked into him
	 * @param ScannedRobotEvent e
	 */
	public void onScannedRobot (ScannedRobotEvent e) {
	    double angleToEnemy = getHeading() + e.getBearing();
	 
	    double radarTurn = Utils.normalRelativeAngleDegrees(angleToEnemy - getRadarHeading() );
	 
	    double extraTurn = Math.min( ( 36.0 / e.getDistance() ), 45);
	 
	    if (radarTurn < 0)
	        radarTurn -= extraTurn;
	    else
	        radarTurn += extraTurn;
	 
	    setTurnRadarRight(radarTurn);
	    
	    enemy.update(e, getHeadingRadians() + e.getBearingRadians(), getX(), getY(), getTime()); 
		
	    move();
	    fire();
	    execute();
	}
	
	/**
	 * Normalizes the angle 
	 * @param angle 
	 * @return angle &isin; [-2pi,2pi]
	 */
	double normalizeBearingRadians(double angle) {
		while (angle >  Math.PI) angle -= 2*Math.PI;
		while (angle < -Math.PI) angle += 2*Math.PI;
		return angle;
	}
	
	public void scan() {
		if (enemy.none()) 
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	}
	
	/**
	 * when it by a bullet change movement type
	 * @param HitBulletEvent
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		if(getTime()-timeWhenDirectionChanged > 25) {
			moveDirection *= -1;
			timeWhenDirectionChanged = getTime();
		}
		setAhead((enemy.getDistance()/4+25)*moveDirection);
	}
	
	/**
	 * Makes sure that the bot doesn't hit walls and characterizes the movement of the robot
	 */
	public void move() {
		if ( ((xmax - getX())<=wallMargin || (ymax - getY())<=wallMargin || getX()<=wallMargin || getY()<=wallMargin) && wallAvoidance<=0) {
			wallAvoidance += 50;
			setMaxVelocity(0); 
			setMaxVelocity(8); 
			moveDirection *=-1;
			timeWhenDirectionChanged = getTime();
			setAhead(500 * moveDirection);
		}
		else {
			wallAvoidance --;
	
			if (enemy.distance<Math.min(xmax, ymax)/10)
				setBack(Math.min(xmax, ymax)/5);
			
			if(getTime()-timeWhenDirectionChanged > 25) { 
				timeWhenDirectionChanged = getTime();
				moveDirection *= -1;
			}

			setTurnRight(normalizeBearingRadians(enemy.getBearing() + Math.PI/2 - (1.5 * moveDirection))); 
			setAhead((enemy.getDistance()/4+25)*moveDirection);
		}
	}
	
	/**
	 * Fire function with bullet prediction
	 */
	public void fire() {
		double Power = Math.min((400 / enemy.getDistance()), 3);
		
		double delta = 0;
		
		double predictedX = enemy.enemyX;
		double predictedY = enemy.enemyY;
		
		double enemyHeading = enemy.getHeading() - oldEnemyHeading;
		
		oldEnemyHeading = enemy.getHeading();
		
		
		while((++delta) * (20.0 - 3.0 * Power) < Point2D.Double.distance(getX(), getY(), predictedX, predictedY)){		
			predictedX += Math.sin(enemy.getHeading()) * enemy.getVelocity();
			predictedY += Math.cos(enemy.getHeading()) * enemy.getVelocity();
			enemy.heading += enemyHeading;
			
			if(	predictedX < 18.0 || predictedY < 18.0 || predictedX > xmax - 18.0 || predictedY > ymax - 18.0){
				predictedX = Math.min(Math.max(18.0, predictedX), xmax - 18.0);
				predictedY = Math.min(Math.max(18.0, predictedY), ymax - 18.0);
				break;
			}
		}
		
		double turn = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
		 
		setTurnGunRightRadians(Utils.normalRelativeAngle(turn - getGunHeadingRadians()));
		
		setFire(Power);
	}
	
	/**
	* Restart everything after the round ends
	*/
	public void onRoundEnded() { 
		enemy = new EnemyRobot();
		xmax = 0;
		ymax = 0;
		wallMargin = 100;
		moveDirection = 1;
		oldEnemyHeading = 0;
	    wallAvoidance = 0;
	    timeWhenDirectionChanged = 0;
	}
}
