import robocode.*;

//obter informacao completa sobre os inimigos apanhados no radar
class EnemyRobot extends AdvancedRobot{
	double enemyX;
	double enemyY;
	double bearing;
	double absoluteBearing;
	double distance;
	double energy;
	double heading;
	double velocity;
	
	public double getEnemyX(){
		return enemyX;		
	}
	
	public double getEnemyY(){
		return enemyY;		
	}
	
	public double getBearing(){
		return bearing;		
	}
	
	public double getAbsoluteBearing(){
		return absoluteBearing;		
	}
	
	public double getDistance(){
		return distance;
	}
	
	public double getEnergy(){
		return energy;
	}
	
	public double getHeading(){
		return heading;
	}
	
	public double getVelocity(){
		return velocity;
	}

	public void update(ScannedRobotEvent bot, double absoluteBearing_, double x, double y, double time){
		bearing = bot.getBearingRadians();
		absoluteBearing = absoluteBearing_;
		
		enemyX = x + bot.getDistance() * Math.sin(absoluteBearing);
		enemyY = y + bot.getDistance() * Math.cos(absoluteBearing);
		
		distance = bot.getDistance();
		energy = bot.getEnergy();
		heading = bot.getHeadingRadians();
		velocity = bot.getVelocity();
	}
	
	public void reset() {
		enemyX = 0.0;
		enemyY = 0.0;
		bearing = 0.0;
		absoluteBearing = 0.0;
		distance = 0.0;
		energy = 0.0;
		heading =0.0;
		velocity = 0.0;
	}
	
	public Boolean none(){
		if (energy == 0.0)
			return true;
		return false;
	}
	
	public EnemyRobot(){
		enemyX = 0.0;
		enemyY = 0.0;
		bearing = 0.0;
		absoluteBearing = 0.0;
		distance = 0.0;
		energy = 0.0;
		heading =0.0;
		velocity = 0.0;
	}
}
