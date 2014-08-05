README
This Repository is the alpha version for Online DataCollection Android App.

DataCollectionOnline is an internal used app to collect audio and context data to train a better DNN model.

DataCollectionOnline is able to collect raw audio data and context data (GPS, deviceId, Orientation, Acceleration,etc) using local device, connect to HYDRA server to send data and poll response text in real-time, All Prompts, partial results and final results are shown on screen. All status are updated real-time and Bluetooth Button is supported.
Layout:

You can swipe between fragments:

Left View is " Status and Prompts", you will see all the real-time updated status for socket connection , recording , the prompt you should read and the response received from the server;
Right View is a BarChartView calculating the current Energy of your speaking: from left to right: background average energy , average energy during speech, background maximum energy, maximum energy during speech Running Average Energy for previous 1 second.
Left View you will see a Button, the text and color of the button indicates the current status and the action you might want to take

How to use the App:
Prerequisites: - Android Version higher than 4.0 - Bluetooth Button (Optional, You can still use button shown on screen if you do not have a bluetooth button )

Steps:

Step1: Start App

Turn on Bluetooth Button, you will see the scan results for all paired bluetooth devices, the click "Connect" to choose the ble button and starts data collection
Choose "Skip Scan" if you do not want to use BLE button

Step2: Server Testing

Follow the hint shown on screen, Click button "Click to Connect to Server" to test whether server is available now
If success, status view will show "Connected To Server" and the button will change to "Click to Start"
If fail, status bar will show "Unable to Connect to Server" If unable to connect to server, please check the following items: - Your device has stable Internet access - The IP Address and Server Port are correct ( You can change the setting using the Actionbar at the topright corner) - The server is correctly running ( Specified Port is available ) - No other user is connected to the server

Step3: Recording

The recording status bar is showing the status:

	Green Speaker icon with message "Recording..." --> currently recording
	Black Non-Speaking icon with message "Stopped Recording" --> not recording

Button on device screen:
	--> Red Button "Click to Start" --> Not recording --> Green Button "Recording" --> Recording

BlueTooth Button:
	Left Button: Press Down --> Holding : Recording Release --> End Recording The button on screen and status view will change simultaneously with BLE button actions
	Right Button: Click --> Switch to next Prompt
You will see Real-time updates the recoginition result of what you speak
After "end recording", the App is will calculate the accuracy of the recognition - If lower than threshold, You will see the same Prompt and read it again - If correct / higher than threshold / You have already repeated it --> Go to the next Prompt

Putting Together:
	 -->Start App --> (Connect BLE Button) 
         --> Click to connect to Server 
         --> See Prompt that you should read on screen 
         --> Click to Start/ BLE LeftKeyDown --> Read the prompt --> Click button again / BLE LeftKeyUp 
         --> Stop recording/ See Results --> Stop App

Installation: You can use Eclipse or other Android Development Tool to install the app Or you can ask me for the signed apk and install it directly.
After installation, you will see a FAT PANDA looking at you, click him and Have FUN~

BTW: it might crash sometimes when you press back to go back to the BLE Activity, I will fix that soon

Best,

Yi

This Repository is the alpha version for Online DataCollection Android App.
More code refine and readme updates will be done in the future