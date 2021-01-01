//
// Created by Florian on 27/11/2020.
//

#ifndef GATERECEIVER_FRAME_H
#define GATERECEIVER_FRAME_H
#include "Arduino.h"
#include "Buffer.h"

class Frame
{

private:
    void printBuffer();

public:
    void parse(char *s, uint16_t size);
    uint8_t protocol;
    Buffer buffer;
};
#endif //GATERECEIVER_FRAME_H
