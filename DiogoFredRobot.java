import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

public class DiogoFredRobot extends AdvancedRobot {
	EnemyRobot enemy = new EnemyRobot();
	double xmax = 0;  //guarda o comprimento do campo
	double ymax = 0;  //guarda a altura do campo
	
	byte moveDirection = 1; //serve para saber quando mudar de direcao
	
	double oldEnemyHeading = 0;
    
    int wallAvoidance = 0; 
    double timeWhenDirectionChanged = 0;
    double wallMargin = 100;

	public void run () {
		xmax = getBattleFieldWidth();
		ymax = getBattleFieldHeight();
		
		setAdjustRadarForGunTurn(true); //tornar o movimento do radar independente
		setAdjustGunForRobotTurn(true); //tornar o movimento da arma independente
		
		turnRadarRightRadians(Double.POSITIVE_INFINITY); //rodar o radar
		
		while(true) {
			scan();
			move();
			fire();
			execute();
		}
	}
	
	public void onScannedRobot (ScannedRobotEvent e) {
		// Angulo até ao inimigo
	    double angleToEnemy = getHeading() + e.getBearing();
	 
	    // Saber o quanto precisamos de virar o radar para encontrar o inimigo
	    double radarTurn = Utils.normalRelativeAngleDegrees(angleToEnemy - getRadarHeading() );
	 
	   //distancia para a qual vamos rodar o radar para cada um dos lados
	    //45 e o maximo que o radar pode rodar num tick
	    double extraTurn = Math.min( ( 36.0 / e.getDistance() ), 45);
	 
	    //Fazer com que o radar avance ainda mais na direcao para o qual vamos virar
	    if (radarTurn < 0)
	        radarTurn -= extraTurn;
	    else
	        radarTurn += extraTurn;
	 
	    //Roda o radar
	    setTurnRadarRight(radarTurn);
	    
	    //atualiza informacao sobre o radar
		enemy.update(e, getHeadingRadians() + e.getBearingRadians(), getX(), getY(), getTime()); 
		
		move();
		fire();
		execute();
	}
	
	//normaliza o angulo de maneira a nao termos de fazer rotacoes superiores a -2pi ou 2pi
	double normalizeBearingRadians(double angle) {
		while (angle >  Math.PI) angle -= 2*Math.PI;
		while (angle < -Math.PI) angle += 2*Math.PI;
		return angle;
	}
	
	public void scan() {
		if (enemy.none()) 
			//se nao encontrarmos o inimigo continuamos a rodar o radar
			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
	}
	
	public void onHitByBullet(HitByBulletEvent e) {
		//se alguma bala nos acertar e nao tivermos mudado de direcao recentemente vamos faze lo
		if(getTime()-timeWhenDirectionChanged > 25) {
			moveDirection *= -1;
			timeWhenDirectionChanged = getTime();
		}
		//movemo-nos para mais perto do inimigo
		setAhead((enemy.getDistance()/4+25)*moveDirection);
	}
	
	public void move() {
		// se estiver perto de uma parede
		if ( ((xmax - getX())<=wallMargin || (ymax - getY())<=wallMargin || getX()<=wallMargin || getY()<=wallMargin) && wallAvoidance<=0) {
			//de maneira a nao estarmos sempre a lidar com o wallAvoidance, pois podiamos entrar em ciclo
			wallAvoidance += 50;
			//parar
			setMaxVelocity(0); 
			setMaxVelocity(8); 
			//mudar de direcao
			moveDirection *=-1;
			 //saber quando mudamos de direcao para nao estarmos sempre a faze-lo
			timeWhenDirectionChanged = getTime();
			//distancia que anda
			setAhead(500 * moveDirection);
		}
		else {
			//para a determinada altura podermos voltar a evitar paredes, pois ja nao ha risco de entrar em ciclo
			wallAvoidance --;
			
			//ver se estamos demasiado perto de um inimigo
			if (enemy.distance<Math.min(xmax, ymax)/10)
				setBack(Math.min(xmax, ymax)/5);
			
			//ver se mudamos de direcao recentemente
			if(getTime()-timeWhenDirectionChanged > 25) { 
				timeWhenDirectionChanged = getTime();
				moveDirection *= -1;
			}
			//manter-nos sempre de frente para o inimigo
			setTurnRight(normalizeBearingRadians(enemy.getBearing() + Math.PI/2 - (1.5 * moveDirection))); 
			setAhead((enemy.getDistance()/4+25)*moveDirection);
		}
	}
	
	public void fire() {
		//aumenta quanto mais perto estivermos do alvo
		double Power = Math.min((400 / enemy.getDistance()), 3);
		//quantos ciclos do while ja fizemos, ou seja, quantas vezes ja calculamos onde o robo estaria
		double delta = 0;
		
		double predictedX = enemy.enemyX;
		double predictedY = enemy.enemyY;
		
		double enemyHeading = enemy.getHeading() - oldEnemyHeading;
		
		oldEnemyHeading = enemy.getHeading();
		
		//dependendo da força da bala, ela chegara mais ou menos depressa ao seu destino, e por isso é que se a distancia entre o nosso
		//robot e o robot inimigo previsto for maior do que a distancia que a bala teria de percorrer para atingir o inimigo previsto,
		//continuamos a calcular para onde deve ir a bala, porque como ela pode viajar muito devagar e o nosso inimigo estar muito longe
		//de nós, temos que prever para onde o inimigo vai mais adiante no futuro
		while((++delta) * (20.0 - 3.0 * Power) < Point2D.Double.distance(getX(), getY(), predictedX, predictedY)){		
			predictedX += Math.sin(enemy.getHeading()) * enemy.getVelocity();
			predictedY += Math.cos(enemy.getHeading()) * enemy.getVelocity();
			enemy.heading += enemyHeading;
			
			//se for previsto que o inimigo irá contra uma parede, é muito provavel que ele se afaste da parede com velocidade contraria
			//e por isso disparamos um bocadinho antes da parede
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
	
	public void onRoundEnded() { //reiniciar status quando a ronda termina
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
