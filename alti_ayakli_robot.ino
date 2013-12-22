#include <Servo.h> 

#include <SoftwareSerial.h>
#define RxD 6
#define TxD 7
 
Servo ortaServo; 
Servo sagBacakServo;
Servo solBacakServo;

int komut=1;

SoftwareSerial blueToothSerial(RxD,TxD);
 
char bluetoothKomut=0;
 
void setup() 
{ 
  pinMode(RxD, INPUT);
  pinMode(TxD, OUTPUT); 
  setupBlueToothConnection(); 

  ortaServo.attach(11); 
  sagBacakServo.attach(12); 
  solBacakServo.attach(13); 
   
  dur();
   
} 
 
 
void loop() 
{ 
  
  if (blueToothSerial.available()){
      bluetoothKomut = blueToothSerial.read(); 
      if( bluetoothKomut == '5' ){
        komut=1;
        ileri();
      }else if(bluetoothKomut=='1'){
        komut=1;
        geri();
      }else{
        komut=0;
        dur();
      }
  }      
} 

void dur(){
  ortaServo.write(40); 
  sagBacakServo.write(40); 
  solBacakServo.write(50); 
}

void ileri(){
  
  ortaServo.write(60);
   for(int pos = 10; pos <= 70; pos += 1)  
  { 
    if(komut==1){
      sagBacakServo.write(pos);   
      solBacakServo.write(pos+20);  
     delay(5);     
    }      
  } 
  ortaServo.write(20);
   for(int pos = 70; pos >= 10; pos -= 1)  
  { 
   if(komut==1){    
      sagBacakServo.write(pos);  
      solBacakServo.write(pos+20);    
     delay(5);     
   }      
  } 
  
  
}

void geri(){
  
  ortaServo.write(60);
   for(int pos = 70; pos >= 10; pos -= 1)  
  {                                   
    sagBacakServo.write(pos);  
    solBacakServo.write(pos+20);    
    delay(5);                       
  } 
  
  ortaServo.write(20);

   for(int pos = 10; pos <= 70; pos += 1)  
  {                                 
    sagBacakServo.write(pos);   
    solBacakServo.write(pos+20);       
    delay(5);                     
  } 
  
}


void setupBlueToothConnection()
{
  blueToothSerial.begin(38400); //Set BluetoothBee BaudRate to default baud rate 38400
  blueToothSerial.print("\r\n+STWMOD=0\r\n"); //set the bluetooth work in slave mode
  blueToothSerial.print("\r\n+STNA=SeeedBTSlave\r\n"); //set the bluetooth name as "SeeedBTSlave"
  blueToothSerial.print("\r\n+STOAUT=1\r\n"); // Permit Paired device to connect me
  blueToothSerial.print("\r\n+STAUTO=0\r\n"); // Auto-connection should be forbidden here
  delay(2000); // This delay is required.
  blueToothSerial.print("\r\n+INQ=1\r\n"); //make the slave bluetooth inquirable 
  Serial.println("The slave bluetooth is inquirable!");
  delay(2000); // This delay is required.
  blueToothSerial.flush();
}

