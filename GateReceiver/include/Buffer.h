//
// Created by Florian on 25/11/2020.
//

#ifndef HEXAPOD_BUFFER_H
#define HEXAPOD_BUFFER_H

#include <Arduino.h>

#define BUFFER_MAX_LENGTH 128

class Buffer {
private:
    uint16_t _length = 0;
    uint16_t getPosition = 0;
    uint16_t putPosition = 0;

public:
    uint8_t array[BUFFER_MAX_LENGTH];
    uint8_t put(uint8_t byte);

    uint8_t put(uint16_t bytes);

    uint8_t put(uint32_t bytes);

    uint8_t put(uint8_t *bytes, unsigned int _length);

    uint8_t get();

    uint16_t getShort();

    float getFloat();

    void reset();

    uint16_t length();
};

#endif //HEXAPOD_BUFFER_H
