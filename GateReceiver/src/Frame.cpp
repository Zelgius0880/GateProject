//
// Created by Florian on 27/11/2020.
//
#include "Frame.h"

void Frame::parse(char *s, uint16_t size)
{
    uint16_t i, j;
    for (i = 0; i < size && s[i] != '['; ++i)
        ;

    ++i;
    char temp[16] = {0};

    for (j = 0; i < size && s[i] != ';'; ++i, ++j)
        temp[j] = s[i];

    temp[j] = '\0';

    protocol = (int)strtol(temp, nullptr, 10);
    memset(temp, 0, sizeof(temp));

    buffer.reset();
    bool stop = false;
    ++i;

    size = min(size, strlen(s));
    Serial.println(size);
    while (!stop)
    {
        for (j = 0; i < size && s[i] != ';' && s[i] != ']'&& j < 16; ++j, ++i)
            temp[j] = s[i];

        stop =  s[i] == ']' || j >= 16;

        if(j < 16){
            int nb = (uint16_t)strtol(temp, nullptr, 10);
            Serial.println(nb);

            buffer.put((uint16_t)nb);

            memset(temp, 0, sizeof(temp));
            ++i;
        } else valid = false;
    }
}

void Frame::printBuffer()
{
    for (uint16_t i = 0; i < buffer.length(); ++i)
    {
        Serial.print((char)buffer.array[i], HEX);
    }
    Serial.println();
}
