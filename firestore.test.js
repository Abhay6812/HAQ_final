const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "farmer_alice_001";
const BOB_UID = "farmer_bob_002";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validSchemePayload(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    name: "PM-KISAN Samman Nidhi",
    shortName: "PM-KISAN",
    department: "Ministry of Agriculture & Farmers Welfare",
    category: "INCOME_SUPPORT",
    benefitSummary: "Direct income support of ₹6,000/year in 3 equal installments.",
    fundingSponsor: "Central Govt • Verified via KVK Pune",
    eligibleStates: ["ALL"],
    maxLandAcres: 5.0,
    maxAnnualIncomeInr: 250000,
    targetGender: "ALL",
    requiredDocuments: [
      "7/12 Land Extract (Satbara)",
      "Income Certificate",
      "Self-Declaration (Zero PII collected on Haq)",
    ],
    officialSourceUrl: "https://pmkisan.gov.in/",
    lastVerifiedDate: "21 Nov 2026",
    verifiedByOrg: "KVK Baramati",
    active: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validCheckLogPayload(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    stateRegion: "Maharashtra",
    inputMode: "PLAIN_LANGUAGE",
    matchedCount: 4,
    checklistDownloaded: false,
    partnerChannel: "KVK Field Desk",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPartnerPayload(uid, ts = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    organizationName: "Maharashtra Dept. of Agriculture",
    partnerType: "STATE_AGRI_DEPT",
    regionCovered: "Maharashtra (36 Districts)",
    farmersSponsoredTarget: 250000,
    fundingContributionLabel: "State Digital Agri Grant • ₹0 Farmer Fee",
    active: true,
    createdAt: ts,
    updatedAt: ts,
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("schemes").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("schemes")
      .doc("pm_kisan")
      .set(validSchemePayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("check_logs").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("partner_funding")
      .get()
  );
});

test("Owner can list, create, update, verify, toggle, and delete schemes with exact Kotlin payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const schemesCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("schemes");

  // Empty subcollection list must succeed
  await assertSucceeds(schemesCol.get());

  // Valid create
  await assertSucceeds(
    schemesCol.doc("pm_kisan").set(validSchemePayload(ALICE_UID))
  );

  // Action 2: Quick verify timestamp refresh
  await assertSucceeds(
    schemesCol.doc("pm_kisan").update({
      lastVerifiedDate: "22 Nov 2026",
      verifiedByOrg: "State Agriculture Office",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Action 3: Toggle active status
  await assertSucceeds(
    schemesCol.doc("pm_kisan").update({
      active: false,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Action 1: Full rule threshold edit
  await assertSucceeds(
    schemesCol.doc("pm_kisan").update({
      maxLandAcres: 2.5,
      maxAnnualIncomeInr: 150000,
      benefitSummary: "Updated ₹6,000/year direct benefit transfer.",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field / @DocumentId 'id' inside body
  await assertFails(
    schemesCol.doc("pm_kisan_bad").set({
      ...validSchemePayload(ALICE_UID),
      id: "pm_kisan_bad",
    })
  );

  // Reject non-https source URL
  await assertFails(
    schemesCol.doc("pm_kisan_http").set({
      ...validSchemePayload(ALICE_UID),
      officialSourceUrl: "http://insecure.example.com",
    })
  );

  // Reject future timestamp
  await assertFails(
    schemesCol
      .doc("pm_kisan_future")
      .set(validSchemePayload(ALICE_UID, new Date(Date.now() + 60000)))
  );

  // Reject immutable createdAt modification
  await assertFails(
    schemesCol.doc("pm_kisan").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Delete succeeds
  await assertSucceeds(schemesCol.doc("pm_kisan").delete());
});

test("Cross-user isolation: Alice cannot read or modify Bob's schemes, logs, or partners", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("users")
      .doc(BOB_UID)
      .collection("schemes")
      .doc("bob_scheme")
      .set(validSchemePayload(BOB_UID));
    await db
      .collection("users")
      .doc(BOB_UID)
      .collection("check_logs")
      .doc("bob_log")
      .set(validCheckLogPayload(BOB_UID));
    await db
      .collection("users")
      .doc(BOB_UID)
      .collection("partner_funding")
      .doc("bob_partner")
      .set(validPartnerPayload(BOB_UID));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("schemes").get()
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(BOB_UID)
      .collection("schemes")
      .doc("bob_scheme")
      .update({ active: false, updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("check_logs").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("partner_funding").get()
  );
});

test("Owner can log zero-PII eligibility checks and mark checklist downloaded", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logsCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("check_logs");

  await assertSucceeds(logsCol.get());
  await assertSucceeds(
    logsCol.doc("log_1").set(validCheckLogPayload(ALICE_UID))
  );
  await assertSucceeds(
    logsCol.doc("log_1").update({
      checklistDownloaded: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost PII field in check log
  await assertFails(
    logsCol.doc("log_pii").set({
      ...validCheckLogPayload(ALICE_UID),
      aadhaarNumber: "123456789012",
    })
  );
});

test("Owner can manage B2B/B2G partner funding records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const partnersCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("partner_funding");

  await assertSucceeds(partnersCol.get());
  await assertSucceeds(
    partnersCol.doc("maharashtra_agri").set(validPartnerPayload(ALICE_UID))
  );
  await assertSucceeds(
    partnersCol.doc("maharashtra_agri").update({
      farmersSponsoredTarget: 300000,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    partnersCol.doc("bad_partner").set({
      ...validPartnerPayload(ALICE_UID),
      partnerType: "FARMER_SUBSCRIPTION_FEE",
    })
  );
});
