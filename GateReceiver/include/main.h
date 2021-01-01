#ifndef UNTITLED_MAIN_H
#define UNTITLED_MAIN_H

#include <Arduino.h>
#include "Frame.h"

const char* SETUP_CMD[] = {"AT+RST\r\n","AT+CWMODE=1\r\n","AT+CIPMUX=0\r\n","AT+CWJAP_CUR=\"U0c5vEPY2xzC3i0WnweR\",\"KGaPoM7bfVzfW5bSyoAaQ\"\r\n","AT+CIPSTART=\"TCP\",\"192.168.4.1\",1000\r\n"};
#define SETUP_CMD_LEGTH 5
#define OPEN1 2
#define OPEN2 5

#define CLOSE1 3
#define CLOSE2 6


void handleResult(char* s, uint16_t size);
void send(char* data, uint16_t size);
void dataReceived(char* s, uint16_t size);
int extractSignal(char* s, uint16_t size);

void open(uint32_t sleep);
void close(uint32_t sleep);
#endif //UNTITLED_MAIN_H
