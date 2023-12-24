#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>

#define F_CPU 8000000UL  // Частота мікроконтролера (8 МГц)
#define BAUD_RATE 9600   // Швидкість передачі через UART
#define SCK 16
#define DS_PIN SCK       // Вивід для DS
#define MOSI 14
#define SHCP_PIN MOSI    // Вивід для SHCP
#define MISO 15
#define STCP_PIN MISO    // Вивід для STCP

void USART_Init() {
  UBRRH = (unsigned char)(F_CPU / (16 * BAUD_RATE) - 1) >> 8;
  UBRRL = (unsigned char)(F_CPU / (16 * BAUD_RATE) - 1);

  UCSRB = (1 << RXEN) | (1 << TXEN);       // Увімкнення передачі та прийому
  UCSRC = (1 << URSEL) | (1 << UCSZ1) | (1 << UCSZ0); // 8 біт, 1 стоп-біт
}

char USART_Receive() {
  while (!(UCSRA & (1 << RXC)))
    ;          // Чекаємо, доки не буде отримано дані
  return UDR; // Повертаємо отриманий байт
}

void USART_Transmit(char data) {
  while (!(UCSRA & (1 << UDRE)))
    ;  // Чекаємо, доки буфер передачі не буде вільний 
  UDR = data; // Відправляємо дані
}

void handleCommand(const char *command, char *res) {
  if (strcmp(command, "ONX") == 0) {
    *res = 255;
    
  } else if (strcmp(command, "OFFX") == 0) {
    *res = 0;
  }
  else {
    for(int i = 0; i < 8; i++) {
      char strOn[5] = {'O', 'N', (char)(i + '0')};
      char strOff[5] = {'O', 'F', ‘F’, (char)(i + '0')};

      if (strcmp(command, strOn) == 0) {
        *res |= (1 << i);
        break;
      }
      else if (strcmp(command, strOff) == 0) {
        *res &= ~(1 << i);
        break;
      }
    }
  }
}

void shiftOut(uint8_t data) {
  for (uint8_t i = 0; i < 8; i++) {
    // Передача біту даних
    if (data & (1 << (7 - i))) {
        PORTB |= (1 << DS_PIN);
    } else {
        PORTB &= ~(1 << DS_PIN);
    }

    // Здійснення зсуву за тактовим сигналом SHCP
    PORTB |= (1 << SHCP_PIN);
    _delay_ms(1); // Невелике затримання для симуляції тактового сигналу
    PORTB &= ~(1 << SHCP_PIN);
  }

  // Збереження даних у реєстрі за тактовим сигналом STCP
  PORTB |= (1 << STCP_PIN);
  _delay_ms(1);
  PORTB &= ~(1 << STCP_PIN);
}

int main(void) {
  // Налаштування виводів мікроконтролера
  DDRB |= (1 << DS_PIN) | (1 << SHCP_PIN) | (1 << STCP_PIN);
  
  USART_Init();
  sei();  // Увімкнення глобальних переривань

  char receivedCommand[5]; // Розмір команди (ON1 або OFF1)
  uint8_t index = 0;
  char lights = 0;

  while (1) {
    char receivedByte = USART_Receive();

    if (receivedByte == '\n') {
      receivedCommand[index] = '\0'; // Додаємо завершуючий нуль
      handleCommand(receivedCommand, &lights);
      shiftOut(lights);
      index = 0; // Скидаємо лічильник індексу для наступної команди
    } else {
      receivedCommand[index++] = receivedByte;
    }
  }

  return 0;
}
