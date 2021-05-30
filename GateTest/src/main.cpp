#include "main.h"

#define outputA 2
#define outputB 4

BTS7960 motor1(9, 7, 8, 6);

int counter = 0;
int aState;
int aLastState;
void setup()
{
    pinMode(outputA, INPUT);
    pinMode(outputB, INPUT);

    Serial.begin(9600);
    // Reads the initial state of the outputA
    aLastState = digitalRead(outputA);

    pinMode(6, OUTPUT);
}
void loop()
{
    aState = digitalRead(outputA); // Reads the "current" state of the outputA
    // If the previous and the current state of the outputA are different, that means a Pulse has occured
    if (aState != aLastState)
    {
        // If the outputB state is different to the outputA state, that means the encoder is rotating clockwise
        if (digitalRead(outputB) != aState)
        {
            counter++;
        }
        else
        {
            counter--;
        }

        if (counter < -255)
            counter = -255;
        else if (counter > 255)
            counter = 255;

        Serial.print("Position: ");
        Serial.println(counter);

        if (counter < 0)
            motor1.TurnLeft(abs(counter));
        else
            motor1.TurnRight(abs(counter));
    }
    aLastState = aState; // Updates the previous state of the outputA with the current state
}