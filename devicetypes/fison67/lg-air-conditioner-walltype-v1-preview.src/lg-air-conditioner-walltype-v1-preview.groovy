/**
 *  LG Air Conditioner v2(v.0.0.2)
 *  LG Air Conditioner Walltype v1(preview) (v0.0.3)
 *
 * MIT License
 *
 * Copyright (c) 2020 fison67@nate.com
 * Copyright (c) 2021 ohung1@naver.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 *	LG Air Conditionor MODEL : SQ06M9JWAN / SQ07P9JWAJ 벽걸이 에어컨
 *
 */
 
import groovy.json.JsonSlurper
import groovy.transform.Field
 
metadata {
	definition (name: "LG Air Conditioner Walltype v1(preview)", namespace: "fison67", author: "fison67", mnmn:"SmartThingsCommunity", vid: "34897825-c025-3bba-8fc6-3852f294dd78",/* ocfDeviceType: "oic.d.airconditioner" */) {
		capability "Switch"
		capability "Air Conditioner Mode"
		capability "Thermostat Cooling Setpoint"
		capability "Fan Speed"
		capability "Refresh"
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
		capability "Dust Sensor"
		capability "Power Meter"


		// status call command
		command "setStatus"
        command "control", ["string", "string"]
        
        // Air Conditioner Mode command
        command "modeChange"
        
        // Air Clean Mode command
		command "airCleanMode"

		// Wind Strenth command
        command "windStrength"

		// air quality attribute pm1
//		attribute "pm1", "number"
	}

	simulator {
	}

	tiles {
		// TODO: define your main and details tiles here
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

// LG connector Config set
def setInfo(String app_url, String address) {
	log.debug "${app_url}, ${address}"
	state.app_url = app_url
    state.id = address
}

def setData(dataList) {
	for(data in dataList){
        state[data.id] = data.code
    }
}

def setStatus(data) {
	log.debug "Update >> ${data.key} >> ${data.data}"
    def jsonObj = new JsonSlurper().parseText(data.data)
    
    if(jsonObj.data.state.reported != null) {
    	def report = jsonObj.data.state.reported
        
        if(report["airState.operation"] != null) {
        	sendEvent(name: "switch", value: report["airState.operation"] == 0 ? "off" : "on")
            def airConditionerMode = report["airState.opMode"]
            if(state.lastOpMode == 0) {
            	airConditionerMode = "cool"
            } else if(state.lastOpMode == 1) {
            	airConditionerMode = "dry"
            } else if(state.lastOpMode == 2) {
            	airConditionerMode = "fanOlny"
			} else if(state.lastOpMode == 3) {
            	airConditionerMode = "auto"
            }
            
            sendEvent(name: "airConditionerMode", value: airConditionerMode)
		}
    
    	if(report["airState.opMode"] != null) {
    		state.lastOpMode = report["airState.opMode"]
        
        	if(device.currentValue("switch") == "on") {
                switch(report["airState.opMode"]) {
                case 0:
                    sendEvent(name: "airConditionerMode", value: "cool")
                    break
                case 1:
                    sendEvent(name: "airConditionerMode", value: "dry")
                    break
				case 2:
                    sendEvent(name: "airConditionerMode", value: "fanOnly")
                    break
                case 3:
                    sendEvent(name: "airConditionerMode", value: "auto")
                    sendEvent(name: "fanSpeed", value: 0)
                    break
				}
			} else {
				sendEvent(name: "airConditionerMode", value : report["airState.opMode"])
			}
		}
	
    	if(report["airState.windStrength"] != null) {
    		if(device.currentValue("switch") == "on") {
        		switch(report["airState.windStrength"]) {
            	case 8:
            		sendEvent(name: "fanSpeed", value: 1)
                	break
				case 2:
            		sendEvent(name: "fanSpeed", value: 2)
                	break
				case 4:
            		sendEvent(name: "fanSpeed", value: 3)
                	break
				case 6:
	            	sendEvent(name: "fanSpeed", value: 4)
    	            break
				}
            } else {
            	sendEvent(name: "fanSpeed", value: 0)
			}
		}

        if(report["airState.tempState.current"] != null) {
        	sendEvent(name: "temperature", value: report["airState.tempState.current"], unit: "C", displayed: false)
        }
        
        if(report["airState.tempState.target"] != null) {
            sendEvent(name: "coolingSetpoint", value: report["airState.tempState.target"])
		}

/*      pm1 is not displayed
        if(report["airState.quality.PM1"] != null) {
            sendEvent(name: "pm1", value: report["airState.quality.PM1"], displayed: false)
        }
*/
        if(report["airState.quality.PM10"] != null) {
            sendEvent(name: "dustLevel", value: report["airState.quality.PM10"], displayed: false)
        }

		if(report["airState.quality.PM2"] != null) {
            sendEvent(name: "fineDustLevel", value: report["airState.quality.PM2"], displayed: false)
        }

//		powermeter, energymeter
		if(report["airState.energy.onCurrent"] != null) {
            sendEvent(name: "power", value: report["airState.energy.onCurrent"], displayed: false) 
		}
/*		energymeter is not used
		if(report["airState.energy.totalCurrent"] != null) {
            sendEvent(name: "engergy", value: report["airState.energy.onCurrent"], displayed: false) 
		}
*/
		if(report["airState.humidity.current"] != null) {
			sendEvent(name: "humidity", value: (report["airState.humidity.current"]/10), displayed: false)
		}
	}    
    updateLastTime();
}
                
                
def installed() {
	log.debug "in installed()"
//	sendEvent(name: "switch", value: "off", displayed: true)
//	sendEvent(name: "coolingSetpoint", value: 26, unit: "C")
	sendEvent(name: "supportedAcModes", value:["auto", "cool", "dry", "fanOnly"])
}
        
def updateLastTime() {
	def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    sendEvent(name: "lastCheckin", value: now, displayed:false)
}


// handle commands
/*
def setFanMode() {
	log.debug "Executing 'setFanMode'"
	// TODO: handle 'setFanMode' command
}
*/

/*
def refresh() {
}
*/

def setAirConditionerMode(mode) {
	// TODO: handle 'setAirConditionerMode' command
    log.debug "setAirConditionerMode " + mode
	if (mode!="cool" && mode!="auto" && mode!="fanOnly" && mode!="dry") {
		sendEvent(name: "airConditionerMode", value: "auto", displayed: true)
		return
	}
	if (device.currentValue("switch") == "on") {
    	switch(mode) {
        case "cool":
        	modeChange(0)
            break
		case "auto":
        	modeChange(3)
            break
		case "dry":
        	modeChange(1)
            break
		case "fanOnly":
        	modeChange(2)
            break
		}
	} else {
    	sendEvent(name: "airConditionerMode", value: mode, displayed: true)
	}
}


def setFanSpeed(strenth) {
	// TODO: handle 'setFanSpeed' command
	log.debug "setFanSpeed" + speed
    if (device.currentValue("switch") == "on") {
    	switch (strenth as Integer)	{
        case 0:
//        	sendEvent(name: "fanSpeed", value: 0)
        	break
		case 1:
        	windStrength(8)
            break
		case 2:
        	windStrength(2)
            break
		case 3:
        	windStrength(4)
            break
		case 4:
        	windStrength(6)
            break
		}
	} else {
		sendEvent(name: "fanSpeed", value: 0)
	}
}

def on() {
    makeCommand('', '{"command":"Operation","ctrlKey":"basicCtrl","dataKey":"airState.operation","dataValue":' + 257 + '}')
}

def off() {
    makeCommand('', '{"command":"Operation","ctrlKey":"basicCtrl","dataKey":"airState.operation","dataValue":' + 0 + '}')
}

def windStrength(val) {
	makeCommand('', '{"command":"Set","ctrlKey":"basicCtrl","dataKey":"airState.windStrength","dataValue":' + val + '}')
}

def setCoolingSetpoint(level) {
	// TODO: handle 'setCoolingSetpoint' command
	if(device.currentValue("switch") == "off") {
        on()   
    } else {
        if(device.currentValue("airConditionerMode")!="dry" && device.currentValue("airConditionerMode")!="fanOnly") {
        	makeCommand('', '{"command":"Set","ctrlKey":"basicCtrl","dataKey":"airState.tempState.target","dataValue":' + level.intValue() + '}')
		} else {
			return
        }
    }
}

def modeChange(val) {
	if(device.currentValue("switch") == "on") {
		makeCommand('', '{"command":"Set","ctrlKey":"basicCtrl","dataKey":"airState.opMode","dataValue":' + val.intValue() + '}')
	}
}

/*
def airCleanMode ()  {

}
*/

// LG connector command action
def control(cmd, value){
	makeCommand(cmd, value)
}

def makeCommand(command, value){
    def body = [
        "id": state.id,
        "command": command,
        "value": value
    ]
    def options = _makeCommand(body)
    sendCommand(options, null)
}

def _makeCommand(body){
	def options = [
     	"method": "POST",
        "path": "/devices/control2",
        "headers": [
        	"HOST": state.app_url,
            "Content-Type": "application/json"
        ],
        "body":body
    ]
    return options
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}