# FRAMES

## Raspberry pi --> ATMega
---
### Open command Packet
<i>Opening/closing a side</i>

[1;{direction};{time};{id}]

- direction: uint16 <br>
0 -> open <br>
1 -> close

- time: unint16. Duration of the during the direction should accours
- id: unint16. <br> 
0 -> Side1<br>
1 -> Side2


## Raspberry pi <-- ATMega
---
### Hello Packet
<i>Send when the connection is established or reset</i>

[0;0]

---
### Config Packet
<i>Configuring the time of the opening of a side</i>

[3;{action};{side}]

- action <br>
0 -> Start<br>
1 -> Stop

- side <br>
0 -> Side1<br>
1 -> Side2
---
### Signal Packet
<i>Send to notify the signal strength</i>

[4;%d] %d: int 0..4

