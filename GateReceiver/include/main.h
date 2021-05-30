#ifndef UNTITLED_MAIN_H
#define UNTITLED_MAIN_H

#include <Arduino.h>
#include "Frame.h"
#include "Gate.h"

const String SSID = "OcvIK7wKlYd7nA";
const String PASSOWRD = "ICiATx7pebzFLg";
const String SERVER_IP = "192.168.4.1";

//const String SSID = "WiFi-2.4-50D0";
//const String PASSOWRD = "tCR3v39Q3cTe";
//const String SERVER_IP = "192.168.1.16";

const String SSID_CMD = "AT+CWJAP_CUR=\"" + SSID + "\",\"" + PASSOWRD + "\"\r\n";
const String CWLAP_CMD = "AT+CWLAP=\"" + SSID + "\"\r\n";
const String CONNECT_SERVER_CMD = "AT+CIPSTART=\"TCP\",\"" + SERVER_IP + "\",1000\r\n";
const char *SETUP_CMD[] = {"AT+RST\r\n", "AT+CWMODE=1\r\n", "AT+CIPMUX=0\r\n", SSID_CMD.c_str(), CONNECT_SERVER_CMD.c_str()};
#define SETUP_CMD_LEGTH 5
#define OPEN1 2
#define OPEN2 5

#define CLOSE1 3
#define CLOSE2 6

#define RED 11
#define GREEN 10

void handleResult(char *s, uint16_t size);
bool send(char *data, uint16_t size);
void dataReceived(char *s, uint16_t size);
int extractSignal(char *s, uint16_t size);
#endif //UNTITLED_MAIN_H
