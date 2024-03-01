/**
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

const functions = require('firebase-functions').region("europe-west2");
const { smarthome } = require('actions-on-google');
const { google } = require('googleapis');
const util = require('util');
const admin = require('firebase-admin');
// Initialize Firebase
admin.initializeApp();
const firestore = admin.firestore();
// Initialize Homegraph
const auth = new google.auth.GoogleAuth({
  scopes: ['https://www.googleapis.com/auth/homegraph'],
});
const homegraph = google.homegraph({
  version: 'v1',
  auth: auth,
});
// Hardcoded user ID
const USER_ID = 'izrbu5AlSo1cXjPtk';
const ACCESS_TOKEN = 'en5kBZ6KLxUcj2RNzvghfW9PmSGTy8ErliV7aQDptC';
const REFRESH_TOKEN = 'rt6LUxW2bKP5OM3vqfeV18NAgYmsncIDEuH7GRjpCJ';
const CLIENT_ID = "UCAchTJ7IxRuqtO5VsDPFf1QeZzNKa32pG0vr4Yjly";
const CLIENT_SECRET = "92NztOLXShUAHc1iwvqpFM7aYdDkIQKVT65eWy8b0u"

exports.login = functions.https.onRequest((request, response) => {
  if (request.method === 'GET') {
    console.log('Requesting login page');
    response.send(`
    <html>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <body>
        <form action="/login" method="post">
          <input type="hidden"
            name="responseurl" value="${request.query.responseurl}" />
          <button type="submit" style="font-size:14pt">
            Link this service to Google
          </button>
        </form>
      </body>
    </html>
  `);
  } else if (request.method === 'POST') {
    // Here, you should validate the user account.
    // In this sample, we do not do that.
    const responseurl = decodeURIComponent(request.body.responseurl);
    console.log(`Redirect to ${responseurl}`);
    return response.redirect(responseurl);
  } else {
    // Unsupported method
    response.send(405, 'Method Not Allowed');
  }
});

exports.fakeauth = functions.https.onRequest((request, response) => {
  const responseurl = util.format('%s?code=%s&state=%s',
    decodeURIComponent(request.query.redirect_uri), 'xxxxxx', request.query.state);
  console.log(`Set redirect as ${responseurl}`);
  return response.redirect(
    `/login?responseurl=${encodeURIComponent(responseurl)}`);
});

exports.faketoken = functions.https.onRequest((request, response) => {
  const grantType = request.query.grant_type ?
    request.query.grant_type : request.body.grant_type;

  const client_secret = request.query.client_secret ?
    request.query.client_secret : request.body.client_secret;

  const client_id = request.query.client_id ?
    request.query.client_id : request.body.client_id;

  const refresh_token = request.query.refresh_token ?
    request.query.refresh_token : request.body.refresh_token;

  const secondsInDay = 86400; // 60 * 60 * 24
  const HTTP_STATUS_OK = 200;
  console.log(`Grant type ${grantType}`);
  console.log(request.query);
  console.log(request.body);

  let obj;
  if (client_secret != CLIENT_SECRET || client_id != CLIENT_ID) {
    response.status(401)
      .json({
        message: "Wrong credentials"
      });
  } else if (grantType === 'authorization_code') {
    obj = {
      token_type: 'bearer',
      access_token: ACCESS_TOKEN,
      refresh_token: REFRESH_TOKEN,
      expires_in: secondsInDay,
    };

    response.status(HTTP_STATUS_OK)
      .json(obj);
  } else if (grantType === 'refresh_token' && refresh_token == REFRESH_TOKEN) {
    obj = {
      token_type: 'bearer',
      access_token: ACCESS_TOKEN,
      expires_in: secondsInDay,
    };

    response.status(HTTP_STATUS_OK)
      .json(obj);
  } else {

    response.status(401)
      .json({
        message: "Wrong token"
      });
  }

});

const app = smarthome();

app.onSync((body) => {
  return {
    requestId: body.requestId,
    payload: {
      agentUserId: USER_ID,
      devices: [{
        id: 'gate',
        type: 'action.devices.types.GATE',
        traits: [
          'action.devices.traits.OpenClose',
        ],
        name: {
          defaultNames: ['My Gate'],
          name: 'gate',
          nicknames: ['Gate'],
        },
        deviceInfo: {
          manufacturer: 'Zeglius',
          model: 'gate-2.0.1',
          hwVersion: '2.0',
          swVersion: '2.0.0',
        },
        willReportState: true,
        attributes: {
          discreteOnlyOpenClose: true,
        },
      }],
    },
  };
});


const queryFirebase = async () => {
  const right = await getValues("states", "gate_right");
  const left = await getValues("states", "gate_left");

  return {
    right: {
      status: right.current,
      progress: right.progress,
      time: right.time
    },
    left: {
      status: left.current,
      progress: left.progress,
      time: left.time
    },
  };
};

const getValues = (collectionName, docName,) => {
  var result;
  return firestore.collection(collectionName).doc(docName).get()
    .then(function (doc) {
      if (doc.exists) {
        result = doc.data();
        return result;
      } else {
        // doc.data() will be undefined in this case
        console.log("No such document!");
        result = "No such document!";
        return result;
      }
    }).catch(function (err) {
      console.log('Error getting documents', err);
    });
};

const computeProgress = (status, percent) => {

  let progress;
  switch (status) {
    case "OPENED":
      progress = 100;
      break;
    case "CLOSED":
      progress = 0;
      break;
    case "CLOSING":
      progress = Math.min(100 - percent, 0);
      break;

    case "OPENING":
      progress = Math.max(percent, 100);
      break;
  }

  return progress;
}

const queryDevice = async (fromQuery) => {
  const data = await queryFirebase();

  const state = fromQuery ? cycleStatus(data) : { openPercent: (computeProgress(data.left.status, data.left.progress) + computeProgress(data.right.status, data.right.progress)) / 2 };

  console.log(state);
  return state;
};


const cycleStatus = (device) => {
  const state = { currentRunCycle: [] }

  if (device.right.status != directions.closed || device.right.status != directions.opened) {
    state.currentRunCycle.push(
      cyclesEN[device.right.status]
    );
    state.currentRunCycle.push(
      cyclesFR[device.right.status]
    );
  } else if (device.left.status != directions.closed || device.left.status != directions.opened) {
    state.currentRunCycle.push(
      cyclesEN[device.left.status]
    );
    state.currentRunCycle.push(
      cyclesFR[device.left.status]
    );
  } else {
    state.currentRunCycle.push(
      cyclesEN[device.left.status]
    );
    state.currentRunCycle.push(
      cyclesFR[device.left.status]
    );
  }

  state.currentTotalRemainingTime = ((device.left.time - device.left.time * device.left.progress / 100) + (device.right.time - device.right.time * device.right.progress / 100)) / 1000;
  state.currentTotalRemainingTime = Math.round(state.currentTotalRemainingTime);
  state.currentCycleRemainingTime = state.currentTotalRemainingTime;

  return state;
}

const cyclesEN = {
  CLOSED: {
    currentCycle: "closed",
    lang: "en"
  },

  OPENED: {
    currentCycle: "opened",
    lang: "en"
  },

  OPENING: {
    currentCycle: "opening",
    nextCycle: "opened",
    lang: "en"
  },

  CLOSING: {
    currentCycle: "closing",
    nextCycle: "closed",
    lang: "en"
  },
}


const cyclesFR = {
  CLOSED: {
    currentCycle: "fermée",
    lang: "fr"
  },

  OPENED: {
    currentCycle: "ouverte",
    lang: "fr"
  },

  OPENING: {
    currentCycle: "ouverture",
    nextCycle: "ouverte",
    lang: "fr"
  },

  CLOSING: {
    currentCycle: "fermeture",
    nextCycle: "fermée",
    lang: "fr"
  },
}

app.onQuery(async (body) => {
  const { requestId } = body;
  const payload = {
    devices: {},
  };
  const queryPromises = [];
  const intent = body.inputs[0];
  for (const device of intent.payload.devices) {
    const deviceId = device.id;
    queryPromises.push(
      queryDevice(false)
        .then((data) => {
          // Add response to device payload
          payload.devices[deviceId] = data;
        }));
  }
  // Wait for all promises to resolve
  await Promise.all(queryPromises);
  return {
    requestId: requestId,
    payload: payload,
  };
});

const directions = {
  closed: "CLOSED",
  opened: "OPENED",
  closing: "CLOSING",
  opening: "OPENING",
}

const updateDevice = async (execution, deviceId) => {
  const { params, command } = execution;

  // Always 'action.devices.commands.OpenClose'

  // True = Open, false = Close
  let direction = params.openPercent > 0;

  let workingStatus = direction ? directions.opening : directions.closing;
  let favorite = await getValues("states", "gate_light");

  favorite = favorite.favorite;
  let favoriteGate = favorite == "Right" ? "right" : "left";

  let otherGate = favorite == "Right" ? "left" : "right";

  let gates = direction ? { first: favoriteGate, second: otherGate } : { first: otherGate, second: favoriteGate };

  firestore.collection("states").doc("gate_" + gates.first).update({ next_could: gates.second, status: workingStatus });

  return {
    openPercent: params.openPercent
  }
};

app.onExecute(async (body) => {
  const { requestId } = body;
  // Execution results are grouped by status
  const result = {
    ids: [],
    status: 'SUCCESS',
    states: {
      online: true,
    },
  };

  const intent = body.inputs[0];
  for (const command of intent.payload.commands) {
    for (const device of command.devices) {
      for (const execution of command.execution) {
        try {
          let data = await updateDevice(execution, device.id)
          result.ids.push(device.id);
          Object.assign(result.states, data);
        }
        catch (e) { console.error('EXECUTE', e) };
      }
    }
  }

  return {
    requestId: requestId,
    payload: {
      commands: [result],
    },
  };
});

app.onDisconnect((body, headers) => {
  console.log('User account unlinked from Google Assistant');
  // Return empty response
  return {};
});

exports.smarthome = functions.https.onRequest(app);

exports.requestsync = functions.https.onRequest(async (request, response) => {
  response.set('Access-Control-Allow-Origin', '*');
  console.info(`Request SYNC for user ${USER_ID}`);
  try {
    const res = await homegraph.devices.requestSync({
      requestBody: {
        agentUserId: USER_ID,
      },
    });
    console.info('Request sync response:', res.status, res.data);
    response.json(res.data);
  } catch (err) {
    console.error(err);
    response.status(500).send(`Error requesting sync: ${err}`);
  }
});

/**
 * Send a REPORT STATE call to the homegraph when data for any device id
 * has been changed.
 */
exports.reportstate = functions.firestore.document('states/{state}').onUpdate(
  async (change, context) => {
    const side = context.params.state;
    if (side != "gate_right" && side != "gate_left") return;

    refreshState();

    const snapshot = change.after.data();

    if (snapshot.next_could != null && snapshot.status == snapshot.current && (snapshot.status == directions.opened || snapshot.status == directions.closed)) {
      const nextSide = snapshot.next_could;
      change.after.ref.update({ next_could: null });
      const next = firestore.collection("states").doc("gate_" + nextSide);
      let gate = await next.get();
      gate = gate.data();

      if (gate.status == snapshot.status) return
      else if (snapshot.status == directions.closed)
        next.update({ status: directions.closing });
      else
        next.update({ status: directions.opening });

    }
  });

const refreshState = async () => {
  console.info('Firebase write event triggered Report State');
  const requestBody = {
    requestId: 'ff36a3cc', /* Any unique ID */
    agentUserId: USER_ID,
    payload: {
      devices: {
        states: {
          /* Report the current state of our gate */
          gate: queryDevice(false),
        },
      },
    },
  };

  const res = await homegraph.devices.reportStateAndNotification({
    requestBody,
  });
  console.info('Report state response:', res.status, res.data);
}