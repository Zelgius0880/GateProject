#include <Arduino.h>

class
    Gate
{
private:
    pin_size_t L_EN, R_EN, LPWM, RPWM;

public:
    Gate(pin_size_t L_EN, pin_size_t R_EN, pin_size_t LPWN, pin_size_t RPWM);
    ~Gate();

    void open(uint32_t sleep);
    void close(uint32_t sleep);

};
