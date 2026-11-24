const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_shinobi_001";
const BOB_UID = "bob_akatsuki_002";
const CHARLIE_UID = "charlie_cyber_003";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validPlayerPayload(uid, created) {
  return {
    userId: uid,
    playerCode: "784553210",
    username: "Harsh007",
    guild: "SHINOBI",
    level: 52,
    xp: 1240,
    rankTitle: "Grand Shinobi",
    coins: 1250,
    diamonds: 540,
    upgradePoints: 8,
    kills: 5,
    deaths: 1,
    worldDistrict: "Sakura City",
    posX: 0.0,
    posY: 0.0,
    posZ: 12.5,
    rotY: 180.0,
    hp: 200.0,
    ep: 200.0,
    shield: 50.0,
    onlineStatus: true,
    aiProxyActive: false,
    aiProxyLastAction: "Patrolling Torii Plaza",
    discoverable: true,
    activeVehicleId: "veh_supercar",
    activePetId: "pet_fox",
    activeWeaponId: "wpn_vortex_ar",
    hairStyle: "Black",
    outfitStyle: "Shadow Blaze",
    accessoryStyle: "Katana",
    accentColorHex: "#FF3B5C",
    equippedAbilities: ["Crimson Slash", "Lightning Dash", "Fox Flame", "Aegis Pulse"],
    unlockedWeapons: ["wpn_vortex_ar", "wpn_pulse_pistol", "wpn_shadow_katana"],
    unlockedVehicles: ["veh_supercar", "veh_moto"],
    unlockedProperties: ["prop_sakura_house"],
    createdAt: created,
    updatedAt: created,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);
  await assertFails(unauthDb.collection("players").where("discoverable", "==", true).get());
  await assertFails(
    unauthDb.collection("players").doc(ALICE_UID).set(validPlayerPayload(ALICE_UID, now))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("pets").get()
  );
  await assertFails(
    unauthDb.collection("bounties").where("participantUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(
    unauthDb.collection("chat_messages").where("roomId", "==", "global_world").get()
  );
});

test("Player profile: owner create, partial update, discoverable list; reject ghost/immutable/cross-user writes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("players").doc(ALICE_UID).set(validPlayerPayload(ALICE_UID, created))
  );

  // Partial update of mutable fields + character customization across outer 2000x2000 districts succeeds
  await assertSucceeds(
    aliceDb.collection("players").doc(ALICE_UID).update({
      coins: 1300,
      worldDistrict: "Coastal Harbor",
      outfitStyle: "Cyber Hunter",
      hairStyle: "Crimson",
      accessoryStyle: "Energy Wings",
      posX: -760.0,
      posZ: 820.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // GPS Location Tracking Service partial update succeeds within valid bounds
  await assertSucceeds(
    aliceDb.collection("players").doc(ALICE_UID).update({
      latitude: 28.6139,
      longitude: 77.2090,
      locationAccuracyMeters: 12.5,
      locationTrackingEnabled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Out-of-bounds latitude fails
  await assertFails(
    aliceDb.collection("players").doc(ALICE_UID).update({
      latitude: 120.0,
      longitude: 77.2090,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user GPS coordinate overwrite fails
  await assertFails(
    bobDb.collection("players").doc(ALICE_UID).update({
      latitude: 19.0760,
      longitude: 72.8777,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("players").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field injection fails
  await assertFails(
    aliceDb.collection("players").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user write fails
  await assertFails(
    bobDb.collection("players").doc(ALICE_UID).update({
      coins: 999999,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Filtered discoverable query succeeds, unfiltered query fails
  await assertSucceeds(
    bobDb.collection("players").where("discoverable", "==", true).get()
  );
  await assertFails(bobDb.collection("players").get());
});

test("User subcollections (pets, vehicles, properties, missions): strict owner isolation & Car Key update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Pet create & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("pets").doc("pet_fox").set({
      userId: ALICE_UID,
      petType: "Anime Fox",
      nickname: "Kurama-Chan",
      rarity: "Mythic",
      level: 12,
      xp: 350,
      skinId: "nine_tails_crimson",
      abilityId: "fox_flame_regen",
      equipped: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Vehicle create & Car Key lock toggle (stolenByUid omitted when null)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_supercar").set({
      userId: ALICE_UID,
      modelId: "Kitsune GT-R",
      category: "supercar",
      paintHex: "#FF1E42",
      skinName: "Sakura Carbon",
      locked: true,
      alarmArmed: true,
      garageSlot: 1,
      upgradeLevel: 3,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("vehicles").doc("veh_supercar").update({
      locked: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Property create & upgrade
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("properties").doc("prop_1").set({
      userId: ALICE_UID,
      propertyType: "villa",
      district: "Sakura City",
      customName: "Indra Clan Villa",
      upgradeLevel: 2,
      decorTheme: "Neo-Tokyo Tatami",
      furnitureCount: 14,
      petAreaEnabled: true,
      garageCapacity: 4,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Mission create & progress
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("missions").doc("mis_1").set({
      userId: ALICE_UID,
      title: "Torii Drift Patrol",
      description: "Drive across Sakura Plaza and defeat 3 rogue bots.",
      category: "ai",
      targetCount: 3,
      progressCount: 1,
      rewardCoins: 150,
      rewardXp: 300,
      completed: false,
      claimed: false,
      aiGenerated: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // HUD Settings create & update owner-isolated
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("hud_settings").doc("default").set({
      userId: ALICE_UID,
      activeLayoutIndex: 2,
      payloadJson: '{"activeLayoutIndex":2}',
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("hud_settings").doc("default").get());

  // Bob cannot list or read Alice's subcollections
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("pets").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vehicles").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("properties").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("missions").get());

  // Reject @DocumentId "id" key injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("pets").doc("pet_bad").set({
      id: "pet_bad",
      userId: ALICE_UID,
      petType: "Dragon",
      nickname: "Ignis",
      rarity: "Epic",
      level: 1,
      xp: 0,
      skinId: "default",
      abilityId: "shield",
      equipped: false,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Bounties & Friendships: participant scoping and terminal state locking", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Create active bounty targeting Bob
  await assertSucceeds(
    aliceDb.collection("bounties").doc("bounty_1").set({
      hunterUid: ALICE_UID,
      hunterName: "Harsh007",
      targetUid: BOB_UID,
      targetName: "ItachiX",
      participantUids: [ALICE_UID, BOB_UID],
      rewardHunter: 50,
      rewardCounter: 25,
      status: "active",
      cooldownUntilEpochMs: Date.now() + 300000,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Participant query succeeds; outsider query fails
  await assertSucceeds(
    bobDb.collection("bounties").where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertFails(charlieDb.collection("bounties").doc("bounty_1").get());

  // Bob defeats Alice first -> target_won
  await assertSucceeds(
    bobDb.collection("bounties").doc("bounty_1").update({
      status: "target_won",
      winnerUid: BOB_UID,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state locking: cannot overwrite once resolved
  await assertFails(
    aliceDb.collection("bounties").doc("bounty_1").update({
      status: "hunter_won",
      winnerUid: ALICE_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("World Chat: roomId filtered query succeeds, unfiltered query fails, immutable after send", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("chat_messages").doc("msg_1").set({
      roomId: "global_world",
      senderUid: ALICE_UID,
      senderName: "Harsh007",
      text: "Meet at Sakura Torii Gate for squad patrol!",
      messageType: "text",
      createdAt: created,
    })
  );

  await assertSucceeds(
    bobDb.collection("chat_messages").where("roomId", "==", "global_world").get()
  );
  await assertFails(bobDb.collection("chat_messages").get());
  await assertFails(
    aliceDb.collection("chat_messages").doc("msg_1").update({ text: "Edited" })
  );
});
