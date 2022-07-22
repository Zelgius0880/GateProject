#include <Arduino.h>

class
    Gate
{
private:
    uint8_t L_EN, R_EN, LPWM, RPWM;

public:
    Gate(uint8_t L_EN, uint8_t R_EN, uint8_t LPWN, uint8_t RPWM);
    ~Gate();

    void open(uint32_t sleep);
    void close(uint32_t sleep);

};
