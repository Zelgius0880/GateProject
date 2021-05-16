#include "main.h"
uint16_t setupCmdIndex = 0;
int cmdResult = 0;
// 0 -> NA
// 1 -> OK
// 2 -> ERROR
// 3 -> CONNECTION CLOSED

bool connected = 0;
bool helloSend = 0;

uint64_t signalSend = 0;

bool startsWith(const char *pre, const char *str)
{
  size_t lenpre = strlen(pre),
         lenstr = strlen(str);
  return lenstr < lenpre ? false : memcmp(pre, str, lenpre) == 0;
}

void reset()
{
  bool connected = 0;
  bool helloSend = 0;
  uint16_t setupCmdIndex = 0;
  int cmdResult = 0;
  Serial1.write("AT+RST\r\n");
  signalSend = 0;
}

void setup()
{
  Serial.begin(115200); // Begin Serial at 2400 baud;

  Serial1.setTimeout(10000);
  Serial1.begin(115200); // Begin Serial at 2400 baud;
  reset();

  pinMode(LED_BUILTIN, OUTPUT);
  pinMode(CLOSE1, OUTPUT);
  pinMode(CLOSE2, OUTPUT);
  pinMode(OPEN1, OUTPUT);
  pinMode(OPEN2, OUTPUT);
}

void loop()
{
  char buffer[128];
  if (Serial1.available() > 0)
  {

    memset(buffer, 0, 128);
    uint16_t size = Serial1.readBytesUntil('\n', buffer, 128);
    buffer[size] = 0;
    handleResult(buffer, size);
  }

  if (setupCmdIndex < SETUP_CMD_LEGTH) // SETUP SECTION
  {
    if (cmdResult == 1)
    {
      setupCmdIndex++;
      if (setupCmdIndex < SETUP_CMD_LEGTH)
      {
        Serial1.write(SETUP_CMD[setupCmdIndex]);
        cmdResult = 0;
      }
    }
    else if (cmdResult == 2)
    {
      Serial.print("Unmanaged error. Reset... ");
      setupCmdIndex = 0;
      cmdResult = 1;
      Serial1.write("AT+RST\r\n");
    }
  }

  //In case of connection reset -> resend HELLO packet
  else if (cmdResult == 1 && !helloSend)
  {
    connected = true;
    cmdResult = 0;
    send("[0;0]\r\n", strlen("[0;0]\r\n"));
  }

  else if (helloSend && connected && (signalSend == 0 || millis() - signalSend > 5 * 60 * 1000))
  {
    //Serial.println(millis(), DEC);
    signalSend = millis();
    delay(100);
    //Serial1.write("AT+CWJAP?\r\n");
    Serial1.write("AT+CWLAP=\"U0c5vEPY2xzC3i0WnweR\"\r\n");
  }
}

void handleResult(char *s, uint16_t size)
{
  Serial.println(s);
  if (startsWith("ready", s))
  {
    cmdResult = 1;
    setupCmdIndex = 0;
  }
  else if (startsWith("OK", s))
    cmdResult = 1;
  else if (startsWith("ERROR", s))
    cmdResult = 2;
  else if (startsWith("+IPD", s))
  {
    cmdResult = 0;
    dataReceived(s, size);
  }

  else if (startsWith("SEND OK", s))
  {
    cmdResult = 0;
    helloSend = true;
  }

  else if (startsWith("WIFI DISCONNECT", s) && setupCmdIndex >= SETUP_CMD_LEGTH)
  {
    reset();
  }

  else if (startsWith("CLOSED", s))
  {
    signalSend = 0;
    cmdResult = 3;
    connected = false;
    helloSend = false;
    Serial1.write("AT+CIPSTART=\"TCP\",\"192.168.4.1\",1000\r\n");
  }

  else if (startsWith("FAIL", s))
  {
    helloSend = false;
    delay(20 * 1000);
    Serial1.write("AT+CWJAP_CUR=\"U0c5vEPY2xzC3i0WnweR\",\"KGaPoM7bfVzfW5bSyoAaQ\"\r\n");
  }
  else if (startsWith("+CWLAP", s))
  {
    char temp[32] = {0};
    Serial1.readBytesUntil('\n', temp, 32); // Reading OK
    Serial.print(temp);
    Serial1.readBytesUntil('\n', temp, 32); // Reading OK
    Serial.print(temp);

    uint32_t signal = extractSignal(s, size);

    delay(500);

    sprintf(temp, "[4;%d]\r\n", signal);
    send(temp, strlen(temp));
  }
}

void dataReceived(char *s, uint16_t size)
{
  Frame f;
  f.parse(s, size);

  switch (f.protocol)
  {
  case 2:

    uint16_t direction =  f.buffer.getShort();
    uint16_t time = f.buffer.getShort();
    uint16_t id = f.buffer.getShort();
    if (direction == 0)
      if (id == 0)
      {
        open1(time);
      }
      else
      {
        open2(time);
      }

    else
    {
      if (id == 0)
      {
        close1(time);
      }
      else
      {
        close2(time);
      }
    }

    break;

  default:
    break;
  }
}

void send(char *data, uint16_t size)
{
  char cmd[64];
  sprintf(cmd, "AT+CIPSEND=%d\r\n", size);
  Serial.write("Sending");
  Serial1.write(cmd);

  uint8_t b = Serial1.read();
  while (b != '\n') //  Loop until the end of the echo cmd
    b = Serial1.read();

  while (b != '\n') //  Loop until the end of the ok answer
    b = Serial1.read();

  while (b != '\n') //  Loop until the end of the ok answer
    b = Serial1.read();

  while (b != '>' && b != '\n')
    b = Serial1.read();

  Serial1.write(data, size);
}

void open1(uint32_t sleep)
{
  Serial.print("Opening during :");
  Serial.println(sleep);

  digitalWrite(CLOSE1, 0);
  digitalWrite(CLOSE2, 0);
  digitalWrite(OPEN1, 1);
  digitalWrite(OPEN2, 0);

  delay(sleep);
  digitalWrite(OPEN1, 0);
}

void close1(uint32_t sleep)
{
  Serial.print("Closing during :");
  Serial.println(sleep);
  digitalWrite(OPEN1, 0);
  digitalWrite(OPEN2, 0);
  digitalWrite(CLOSE1, 1);
  digitalWrite(CLOSE2, 0);

  delay(sleep);
  digitalWrite(CLOSE1, 0);
}

void open2(uint32_t sleep)
{
  Serial.print("Opening during :");
  Serial.println(sleep);

  digitalWrite(CLOSE1, 0);
  digitalWrite(CLOSE2, 0);
  digitalWrite(OPEN1, 0);
  digitalWrite(OPEN2, 1);

  delay(sleep);
  digitalWrite(OPEN2, 0);
}

void close2(uint32_t sleep)
{
  Serial.print("Closing during :");
  Serial.println(sleep);
  digitalWrite(OPEN1, 0);
  digitalWrite(OPEN2, 0);
  digitalWrite(CLOSE1, 0);
  digitalWrite(CLOSE2, 1);

  delay(sleep);
  digitalWrite(CLOSE2, 0);
}

int extractSignal(char *s, uint16_t size)
{

  // +CWLAP:(2,"U0c5vEPY2xzC3i0WnweR",-16,"ca:2b:96:00:9a:de",5,91,0)

  uint16_t i, j;
  char temp[32] = {0};
  //+CWLAP:(
  for (i = 0; i < size && s[i] != '('; ++i)
    ;

  //<enc>
  ++i;
  for (j = 0; i < size && s[i] != ','; ++i, ++j)
    temp[j] = s[i];
  temp[j] = '\0';

  //<ssid>
  ++i;
  for (j = 0; i < size && s[i] != ','; ++i, ++j)
    temp[j] = s[i];
  temp[j] = '\0';

  memset(temp, 0, sizeof(temp));

  //<rssi>
  ++i;
  for (j = 0; i < size && s[i] != ','; ++i, ++j)
    temp[j] = s[i];
  temp[j] = '\0';

  return (int)strtol(temp, nullptr, 10);
}
