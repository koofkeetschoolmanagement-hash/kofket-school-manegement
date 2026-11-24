const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_fayera";
const TEACHER_UID = "teacher_tolasa";
const PARENT_UID = "parent_abebe";
const STUDENT_A_UID = "student_chaltu";
const STUDENT_B_UID = "student_bikila";
const SCHOOL_ID = "koofkeet_main";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

async function seedRoles() {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("school_meta").doc("initialized").set({
      schoolId: SCHOOL_ID,
      adminUid: ADMIN_UID,
      adminEmail: "koofkeetschoolmanagement@gmail.com",
      schoolName: "Mana Barumsaa Koofkeet",
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: "koofkeetschoolmanagement@gmail.com",
      createdAt: past,
    });
    await db.collection("users").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      displayName: "Admin Fayyeeraa",
      email: "koofkeetschoolmanagement@gmail.com",
      role: "ADMIN",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(TEACHER_UID).set({
      userId: TEACHER_UID,
      displayName: "Barsiisaa Tolasaa",
      email: "teacher@koofkeet.org",
      role: "TEACHER",
      subject: "Herrega",
      gradeLevel: "Kutaa 10",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(PARENT_UID).set({
      userId: PARENT_UID,
      displayName: "Maatii Ababaa",
      email: "parent@koofkeet.org",
      role: "PARENT",
      linkedStudentId: STUDENT_A_UID,
      linkedStudentName: "Caaltuu",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(STUDENT_A_UID).set({
      userId: STUDENT_A_UID,
      displayName: "Caaltuu",
      email: "chaltu@koofkeet.org",
      role: "STUDENT",
      gradeLevel: "Kutaa 10",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(STUDENT_B_UID).set({
      userId: STUDENT_B_UID,
      displayName: "Biqilaa",
      email: "bikila@koofkeet.org",
      role: "STUDENT",
      gradeLevel: "Kutaa 10",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    });
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  await seedRoles();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("announcements").where("schoolId", "==", SCHOOL_ID).get());
  await assertFails(unauthDb.collection("exam_results").where("studentId", "==", STUDENT_A_UID).get());
  await assertFails(unauthDb.collection("users").doc("hacker").set({
    userId: "hacker",
    displayName: "Hacker",
    email: "h@x.com",
    role: "ADMIN",
    adminId: ADMIN_UID,
    createdAt: new Date(Date.now() - 1000),
    updatedAt: new Date(Date.now() - 1000),
  }));
});

test("Students cannot self-escalate role to ADMIN or TEACHER on creation or update", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 2000);
  const attackerDb = testEnv.authenticatedContext("new_user_1", {
    email: "new_student@gmail.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    attackerDb.collection("users").doc("new_user_1").set({
      userId: "new_user_1",
      displayName: "Fake Admin",
      email: "new_student@gmail.com",
      role: "ADMIN",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    attackerDb.collection("users").doc("new_user_1").set({
      userId: "new_user_1",
      displayName: "Barataa Haaraa",
      email: "new_student@gmail.com",
      role: "STUDENT",
      gradeLevel: "Kutaa 9",
      adminId: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cannot escalate own role via update
  await assertFails(
    attackerDb.collection("users").doc("new_user_1").update({
      role: "TEACHER",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Student A cannot read Student B exam results; Student A & Parent can read Student A results", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 5000);
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();

  await assertSucceeds(
    teacherDb.collection("exam_results").doc("res_1").set({
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      parentId: PARENT_UID,
      teacherId: TEACHER_UID,
      teacherName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      subject: "Herrega",
      gradeLevel: "Kutaa 10",
      examName: "Qormaata 1ffaa",
      monthLabel: "Sadaasa 2026",
      score: 88.5,
      maxScore: 100.0,
      remarks: "Gaariidha",
      createdAt: past,
      updatedAt: past,
    })
  );

  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();

  // Student A can read own results via scoped query
  await assertSucceeds(studentADb.collection("exam_results").where("studentId", "==", STUDENT_A_UID).get());
  // Parent can read child's results via scoped query
  await assertSucceeds(parentDb.collection("exam_results").where("parentId", "==", PARENT_UID).get());
  // Student B is strictly denied from reading Student A's results or unfiltered list
  await assertFails(studentBDb.collection("exam_results").doc("res_1").get());
  await assertFails(studentBDb.collection("exam_results").where("studentId", "==", STUDENT_A_UID).get());
  await assertFails(studentBDb.collection("exam_results").get());
});

test("Parent-Teacher and Parent-Admin messages are strictly hidden from Students", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 5000);
  const parentDb = testEnv.authenticatedContext(PARENT_UID).firestore();
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();

  await assertSucceeds(
    parentDb.collection("parent_messages").doc("msg_1").set({
      parentId: PARENT_UID,
      parentName: "Maatii Ababaa",
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      recipientRole: "TEACHER",
      recipientId: TEACHER_UID,
      participantUids: [PARENT_UID, TEACHER_UID],
      senderId: PARENT_UID,
      senderName: "Maatii Ababaa",
      senderRole: "PARENT",
      subject: "Haala Barnootaa",
      body: "Barnoonni Caaltuu akkam jira?",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Parent and Teacher can read via participantUids query
  await assertSucceeds(parentDb.collection("parent_messages").where("participantUids", "array-contains", PARENT_UID).get());
  await assertSucceeds(teacherDb.collection("parent_messages").where("participantUids", "array-contains", TEACHER_UID).get());

  // Student A CANNOT read or list or create parent_messages even about themselves!
  await assertFails(studentADb.collection("parent_messages").doc("msg_1").get());
  await assertFails(studentADb.collection("parent_messages").where("participantUids", "array-contains", PARENT_UID).get());
  await assertFails(studentADb.collection("parent_messages").where("parentId", "==", PARENT_UID).get());
  await assertFails(studentADb.collection("parent_messages").get());
  const ownQuerySnap = await assertSucceeds(
    studentADb.collection("parent_messages").where("participantUids", "array-contains", STUDENT_A_UID).get()
  );
  if (ownQuerySnap.size !== 0) {
    throw new Error("Expected 0 parent messages visible to student");
  }
  await assertFails(
    studentADb.collection("parent_messages").doc("msg_2").set({
      parentId: PARENT_UID,
      parentName: "Maatii Ababaa",
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      recipientRole: "TEACHER",
      recipientId: TEACHER_UID,
      participantUids: [PARENT_UID, TEACHER_UID, STUDENT_A_UID],
      senderId: STUDENT_A_UID,
      senderName: "Caaltuu",
      senderRole: "PARENT",
      subject: "Sneaky",
      body: "Trying to inject myself",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Early warnings can be created/resolved by Teacher/Admin and are hidden from Students", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 5000);
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "koofkeetschoolmanagement@gmail.com",
    email_verified: true,
  }).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();

  await assertSucceeds(
    teacherDb.collection("early_warnings").doc("warn_1").set({
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      gradeLevel: "Kutaa 10",
      subject: "Herrega",
      previousAverage: 85.0,
      currentScore: 48.0,
      dropAmount: 37.0,
      reason: "Qabxiin 85.0% irraa gara 48.0% gadi bu'eera.",
      teacherId: TEACHER_UID,
      adminId: ADMIN_UID,
      status: "OPEN",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(teacherDb.collection("early_warnings").where("teacherId", "==", TEACHER_UID).get());
  await assertSucceeds(adminDb.collection("early_warnings").where("adminId", "==", ADMIN_UID).get());
  await assertSucceeds(adminDb.collection("early_warnings").doc("warn_1").update({
    status: "RESOLVED",
    updatedAt: new Date(Date.now() - 1000),
  }));

  // Student A cannot read or list early warnings
  await assertFails(studentADb.collection("early_warnings").doc("warn_1").get());
  await assertFails(studentADb.collection("early_warnings").where("studentId", "==", STUDENT_A_UID).get());
  await assertFails(studentADb.collection("early_warnings").get());
});

test("Assignments, submissions, quizzes, materials, announcements, and shadow-update/timestamp guards", async () => {
  await seedRoles();
  const past = new Date(Date.now() - 5000);
  const future = new Date(Date.now() + 60000);
  const teacherDb = testEnv.authenticatedContext(TEACHER_UID).firestore();
  const studentADb = testEnv.authenticatedContext(STUDENT_A_UID).firestore();
  const studentBDb = testEnv.authenticatedContext(STUDENT_B_UID).firestore();

  // Teacher creates assignment
  await assertSucceeds(
    teacherDb.collection("assignments").doc("hw_1").set({
      title: "Hojii Manaa Aljebraa",
      description: "Gaaffilee 1-10 hojjedhaa",
      subject: "Herrega",
      gradeLevel: "Kutaa 10",
      dueDateLabel: "2026-11-30",
      teacherId: TEACHER_UID,
      teacherName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      schoolId: SCHOOL_ID,
      status: "OPEN",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Student A submits assignment (no @DocumentId `id` field, valid past timestamp)
  await assertSucceeds(
    studentADb.collection("assignment_submissions").doc("sub_1").set({
      assignmentId: "hw_1",
      assignmentTitle: "Hojii Manaa Aljebraa",
      subject: "Herrega",
      studentId: STUDENT_A_UID,
      studentName: "Caaltuu",
      parentId: PARENT_UID,
      teacherId: TEACHER_UID,
      adminId: ADMIN_UID,
      answerText: "x = 5, y = 12",
      score: 0.0,
      status: "SUBMITTED",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Student B cannot read Student A submission
  await assertFails(studentBDb.collection("assignment_submissions").doc("sub_1").get());

  // Teacher grades submission with partial update
  await assertSucceeds(
    teacherDb.collection("assignment_submissions").doc("sub_1").update({
      score: 95.0,
      feedback: "Baay'ee gaarii!",
      status: "GRADED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update & extra `id` field & future timestamp rejected
  await assertFails(
    teacherDb.collection("announcements").doc("ann_bad").set({
      id: "ann_bad",
      title: "Extra ID",
      body: "Should fail",
      category: "GENERAL",
      authorId: TEACHER_UID,
      authorName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      schoolId: SCHOOL_ID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    teacherDb.collection("announcements").doc("ann_future").set({
      title: "Future Time",
      body: "Should fail",
      category: "GENERAL",
      authorId: TEACHER_UID,
      authorName: "Barsiisaa Tolasaa",
      adminId: ADMIN_UID,
      schoolId: SCHOOL_ID,
      createdAt: future,
      updatedAt: future,
    })
  );
});
