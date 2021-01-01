//
// Created by Florian on 25/11/2020.
//

#include "Buffer.h"

uint8_t Buffer::put(uint8_t byte) {
    array[putPosition] = byte;
    ++putPosition;
    ++_length;

    if (putPosition >= BUFFER_MAX_LENGTH)
        putPosition = 0;

    return 1;
}

uint8_t Buffer::put(uint16_t bytes) {
    if (putPosition + 2 >= BUFFER_MAX_LENGTH) return 0;
    put((uint8_t) (bytes & 0xffu));
    put((uint8_t) ((bytes & 0xff00u) >> 8u));
    return 1;
}

uint8_t Buffer::put(uint32_t bytes) {
    if (putPosition + 4 >= BUFFER_MAX_LENGTH) return 0;
    put((uint8_t) (bytes & 0xffu));
    put((uint8_t) ((bytes & 0xff00u) >> 8u));
    put((uint8_t) ((bytes & 0xff0000u) >> 16u));
    put((uint8_t) ((bytes & 0xff000000u) >> 24u));
    return 1;
}

uint8_t Buffer::put(uint8_t *bytes, unsigned int length) {
    if (putPosition + length >= BUFFER_MAX_LENGTH) return 0;

    for (uint16_t i = 0; i < length; ++i) {
        put(bytes[i]);
    }

    return 1;
}

uint8_t Buffer::get() {
    uint8_t v = array[getPosition];
    ++getPosition;
    if (getPosition >= BUFFER_MAX_LENGTH)
        getPosition = 0;
    return v;
}

uint16_t Buffer::getShort() {
    uint16_t v = array[getPosition]
                 | array[getPosition + 1] << 8u;

    getPosition += 2;
    if (getPosition >= BUFFER_MAX_LENGTH)
        getPosition = 0;
    return v;
}

float Buffer::getFloat() {
    float v = ((uint32_t) array[getPosition])
              | ((uint32_t) array[getPosition + 1]) << 8u
              | ((uint32_t) array[getPosition + 2]) << 16u
              | ((uint32_t) array[getPosition + 3]) << 24u;

    getPosition += 4;
    if (getPosition >= BUFFER_MAX_LENGTH)
        getPosition = 0;

    return v;
}

void Buffer::reset() {
    _length = 0;
    putPosition = 0;
    getPosition = 0;
}

uint16_t Buffer::length() {
    return _length;
}
