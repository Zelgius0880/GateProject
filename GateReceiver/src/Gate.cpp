#include "Gate.h"

Gate::Gate(pin_size_t L_EN, pin_size_t R_EN, pin_size_t LPWN, pin_size_t RPWM)
{
    this->L_EN = L_EN;
    this->R_EN = R_EN;
    this->LPWM = LPWN;
    this->RPWM = RPWM;

    pinMode(L_EN, OUTPUT);
    pinMode(R_EN, OUTPUT);
    pinMode(LPWN, OUTPUT);
    pinMode(RPWM, OUTPUT);

    digitalWrite(L_EN, HIGH);
    digitalWrite(R_EN, HIGH);
}

Gate::~Gate()
{
}

void Gate::close(uint32_t sleep)
{
    analogWrite(RPWM, 255);
    analogWrite(LPWM, 0);

    delay(sleep);

    analogWrite(RPWM, 0);
}

void Gate::open(uint32_t sleep)
{
    analogWrite(LPWM, 255);
    analogWrite(RPWM, 0);

     delay(sleep);

    analogWrite(LPWM, 0);
}