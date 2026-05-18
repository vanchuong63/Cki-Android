#include <Ultrasonic.h>

const int trigL = 22;
const int echoL = 23;

const int trigF = 24;
const int echoF = 25;

const int trigR = 26;
const int echoR = 27;

const int in1 = 4;
const int in2 = 5;

const int in3 = 6;
const int in4 = 7;

const int enA = 2;
const int enB = 3;

#define PWM 200
#define DIS 15
void setup()
{
  pinMode(trigL,OUTPUT);
  pinMode(echoL, INPUT);

  pinMode(trigF,OUTPUT);
  pinMode(echoF, INPUT);

  pinMode(trigR,OUTPUT);
  pinMode(echoR, INPUT);

  pinMode(in1, OUTPUT);
  pinMode(in2, OUTPUT);

  pinMode(in3, OUTPUT);
  pinMode(in4, OUTPUT);

  pinMode(enA, OUTPUT);
  pinMode(enB, OUTPUT);

  Serial.begin(9600);
}

void loop() {
  long front = FrontSensor();
  delay(20);

  long right = RightSensor();
  delay(20);

  long left = LeftSensor();
  delay(20);
  if (front() < DIS && right() < DIS && left() < DIS) {
    turn_right () ;
    delay(3000);
  }
  else if(front() < DIS && right() < DIS && left() > DIS) {
    turn_left () ;
  }
  else if (front() < DIS && right() > DIS && left() < DIS) {
    turn_right();
  }
  else if (front() < DIS && right() > DIS && left() > DIS) {
    turn_right();
  }
  else if (front() > DIS && right() > DIS && left() < DIS) {
    turn_right();

    delay(180);
    forward();
  }
  else if (front() > DIS && right() < DIS && left() > DIS) {
    turn_left();

    delay(180);
    forward();
  }
  else {
    forward();
  }
 
}

void forward()
{
  digitalWrite(in1,HIGH);
  digitalWrite(in2,LOW);
  digitalWrite(in3,HIGH);
  digitalWrite(in4,LOW);
  analogWrite(enA, PWM);
  analogWrite(enB, PWM);

}
void turn_left() 
{
  digitalWrite(in1,LOW);
  digitalWrite(in2,HIGH);
  digitalWrite(in3,HIGH);
  digitalWrite(in4,LOW);
  analogWrite(enA, PWM);
  analogWrite(enB, PWM);

}
void turn_right() 
{
  digitalWrite(in1,HIGH);
  digitalWrite(in2,LOW);
  digitalWrite(in3,LOW);
  digitalWrite(in4,HIGH);
  analogWrite(enA, PWM);
  analogWrite(enB, PWM);
}
void reverse ()
{
  digitalWrite(in1,LOW);
  digitalWrite(in2,HIGH);
  digitalWrite(in3,LOW);
  digitalWrite(in4,HIGH);
  analogWrite(enA, PWM);
  analogWrite(enB, PWM);
}
void stop_motor()
{
  digitalWrite(in1,LOW);
  digitalWrite(in2,LOW);
  digitalWrite(in3,LOW);
  digitalWrite(in4,LOW);
  analogWrite(enA, LOW);
  analogWrite(enB, LOW);
}
long FrontSensor() {
  long dur;

  digitalWrite(trigF, LOW);

  delayMicroseconds(5);

  digitalWrite(trigF, HIGH);
  delayMicroseconds(10);

  digitalWrite(trigF, LOW);

  dur = pulseIn(echoF,HIGH);
  return (dur/58);
}

long RightSensor() {
  long dur;
  digitalWrite(trigR, LOW);

  delayMicroseconds(5);

  digitalWrite(trigR, HIGH);
  delayMicroseconds(10);

  digitalWrite(trigR, LOW);

  dur = pulseIn(echoR,HIGH);
  return (dur/58);
}

long LeftSensor() {
  long dur;
  digitalWrite(trigL, LOW);

  delayMicroseconds(5);

  digitalWrite(trigL, HIGH);
  delayMicroseconds(10);

  digitalWrite(trigL, LOW);

  dur = pulseIn(echoL,HIGH);
  return (dur/58);
}













