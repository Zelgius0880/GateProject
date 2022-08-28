#include "main.h"

void setup()
{
  pinMode(OPEN_L, INPUT);
  pinMode(OPEN_R, INPUT);
  pinMode(CLOSE_L, INPUT);
  pinMode(CLOSE_R, INPUT);

  pinMode(LPWM_LEFT, OUTPUT);
  pinMode(RPWM_LEFT, OUTPUT);

  pinMode(LPWM_RIGHT, OUTPUT);
  pinMode(RPWM_RIGHT, OUTPUT);

  digitalWrite(OPEN_L, LOW);
  digitalWrite(OPEN_R, LOW);
  digitalWrite(CLOSE_L, LOW);
  digitalWrite(CLOSE_R, LOW);

  Serial.begin(115200);
}

PinStatus closeL = LOW;
PinStatus openL = LOW;

PinStatus closeR = LOW;
PinStatus openR = LOW;

void loop()
{
  PinStatus status;

  status = digitalRead(CLOSE_L);
  if (status != closeL)
  {
    closeL = status,
    digitalWrite(LPWM_LEFT, closeL );
  }
  
  status = digitalRead(OPEN_L);
  if (status != openL)
  {
    openL = status,
    digitalWrite(RPWM_LEFT, openL);
  }

  status = digitalRead(CLOSE_R);
  if (status != closeR)
  {
    closeR = status,
    digitalWrite(LPWM_RIGHT, closeR);
  }
  
  status = digitalRead(OPEN_R);
  if (status != openR)
  {
    openR = status,
    digitalWrite(RPWM_RIGHT, openR);
  }

  //delay(1000);
}
