-- Sample Seed Data for Online Fitness Coaching Platform
-- Compatible with both SQLite and MySQL

-- Users: Admin, Coaches, Users
INSERT INTO users (id, name, email, password, role, status, bio) VALUES
(1, 'System Administrator', 'admin@fitness.com', 'admin123', 'ADMIN', 'ACTIVE', 'Platform Administrator with system-wide privileges.'),
(2, 'Marcus Vance', 'coach.marcus@fitness.com', 'coach123', 'COACH', 'ACTIVE', 'Certified CSCS Strength Coach with 10+ years experience in powerlifting & hypertrophy.'),
(3, 'Elena Rostova', 'coach.elena@fitness.com', 'coach123', 'COACH', 'ACTIVE', 'Master Trainer specializing in HIIT, metabolic conditioning, and mobility training.'),
(4, 'John Doe', 'john.doe@gmail.com', 'user123', 'USER', 'ACTIVE', 'Fitness enthusiast aiming for sustainable fat loss and cardiovascular endurance.'),
(5, 'Sarah Connor', 'sarah.connor@gmail.com', 'user123', 'USER', 'ACTIVE', 'Athlete training for lean muscle development and tactical strength.'),
(6, 'Alex Smith', 'alex.smith@gmail.com', 'user123', 'USER', 'ACTIVE', 'Beginner getting started on healthy lifestyle habits and flexibility.');

-- User Profiles
INSERT INTO user_profiles (user_id, age, gender, height, current_weight, target_weight, fitness_goal) VALUES
(4, 28, 'Male', 178.0, 84.5, 78.0, 'Fat Loss & Athletic Conditioning'),
(5, 31, 'Female', 168.0, 62.0, 65.0, 'Lean Muscle Hypertrophy & Strength'),
(6, 24, 'Other', 172.0, 70.0, 68.0, 'General Stamina & Joint Mobility');

-- Workout Plans
INSERT INTO workout_plans (id, coach_id, title, description, category, difficulty, duration_weeks, approval_status, moderation_notes) VALUES
(1, 2, '12-Week Shred & Power', 'Progressive resistance training combined with strategic metabolic conditioning for maximum fat oxidation and muscle retention.', 'Fat Loss', 'INTERMEDIATE', 12, 'APPROVED', 'Verified by admin. Excellent structure.'),
(2, 2, 'Iron Forge Hypertrophy', 'Focus on compound lifts, progressive overload, and high volume target sets for chest, back, and leg development.', 'Muscle Building', 'ADVANCED', 8, 'APPROVED', 'Approved for advanced trainees.'),
(3, 3, 'HIIT Ignition 30', 'Daily 30-minute high intensity interval training sessions requiring minimal gym equipment for rapid endurance gains.', 'HIIT Cardio', 'BEGINNER', 4, 'APPROVED', 'Verified suitable for beginner level.'),
(4, 3, 'Mobility & Core Flow', 'Holistic flow focusing on spinal decompression, hip openers, and deep core neuromuscular control.', 'Yoga & Mobility', 'BEGINNER', 6, 'PENDING', 'Pending safety review of exercise timings.'),
(5, 2, 'Spartan Beast Split', 'Intense 6-day split designed for peak aesthetic conditioning and power output.', 'Muscle Building', 'ADVANCED', 10, 'PENDING', 'New submission awaiting content moderation.');

-- Plan Exercises
INSERT INTO plan_exercises (plan_id, day_of_week, exercise_name, sets, reps, duration_mins, rest_seconds, notes) VALUES
(1, 'Day 1 - Lower Body Power', 'Barbell Back Squats', 4, 8, 15, 90, 'Focus on hip drive and depth below parallel.'),
(1, 'Day 1 - Lower Body Power', 'Romanian Deadlifts', 3, 10, 10, 75, 'Feel the hamstring stretch, keep spine neutral.'),
(1, 'Day 1 - Lower Body Power', 'Walking Dumbbell Lunges', 3, 12, 10, 60, '12 reps per leg with upright posture.'),
(1, 'Day 1 - Lower Body Power', 'Hanging Knee Raises', 3, 15, 8, 45, 'Control the swing, engage lower abdominals.'),
(1, 'Day 2 - Upper Body Push', 'Barbell Bench Press', 4, 8, 15, 90, 'Touch sternum with controlled tempo.'),
(1, 'Day 2 - Upper Body Push', 'Incline Dumbbell Press', 3, 10, 12, 75, '30-degree bench angle for upper pec activation.'),
(1, 'Day 2 - Upper Body Push', 'Lateral Dumbbell Raises', 4, 15, 10, 45, 'Lead with elbows, slight forward lean.'),
(1, 'Day 2 - Upper Body Push', 'Cable Tricep Pushdowns', 3, 12, 8, 45, 'Flare out slightly at the bottom contraction.'),
(1, 'Day 3 - Upper Body Pull', 'Conventional Deadlifts', 4, 6, 20, 120, 'Reset between reps, brace abdominal core.'),
(1, 'Day 3 - Upper Body Pull', 'Weighted Pull-Ups', 4, 8, 12, 90, 'Full extension at bottom to chin over bar.'),
(1, 'Day 3 - Upper Body Pull', 'Bent-Over Barbell Rows', 3, 10, 12, 75, 'Pull barbell to upper navel with flat back.'),
(1, 'Day 3 - Upper Body Pull', 'Alternating Dumbbell Curls', 3, 12, 8, 60, 'Supinate wrists at top of movement.'),
(2, 'Day 1 - Chest & Triceps', 'Incline Barbell Bench Press', 4, 8, 15, 90, 'Explosive ascent, 3-second eccentric.'),
(2, 'Day 1 - Chest & Triceps', 'Flat Dumbbell Flyes', 3, 12, 10, 60, 'Deep stretch without hyperextending shoulders.'),
(2, 'Day 1 - Chest & Triceps', 'Dips (Weighted)', 3, 10, 10, 75, 'Lean forward 15 degrees for chest bias.'),
(3, 'Day 1 - Full Body Blast', 'Burpees Over Bar', 4, 15, 10, 45, 'Maintain steady cadence and explosive jump.'),
(3, 'Day 1 - Full Body Blast', 'Kettlebell Swings', 4, 20, 10, 45, 'Hip hinge movement, squeeze glutes at top.'),
(3, 'Day 1 - Full Body Blast', 'Mountain Climbers', 4, 30, 8, 30, 'Keep hips low and drive knees rapidly.');

-- Enrollments
INSERT INTO user_plan_enrollments (id, user_id, plan_id, enrolled_at, status) VALUES
(1, 4, 1, '2026-09-10 08:30:00', 'ACTIVE'),
(2, 5, 2, '2026-09-12 10:15:00', 'ACTIVE'),
(3, 6, 3, '2026-09-15 14:00:00', 'ACTIVE');

-- Workout Progress Logs
INSERT INTO workout_logs (user_id, plan_id, log_date, duration_minutes, calories_burned, weight, notes) VALUES
(4, 1, '2026-09-16', 55, 480, 84.5, 'Day 1 Lower body completed. Felt strong on squats.'),
(4, 1, '2026-09-18', 50, 430, 84.2, 'Day 2 Push day. Increased bench press by 2.5kg.'),
(4, 1, '2026-09-20', 60, 520, 83.8, 'Day 3 Pull day. Deadlifts felt solid, great energy.'),
(4, 1, '2026-09-23', 55, 490, 83.5, 'Squats 4x8 at 90kg. Hamstrings felt engaged.'),
(4, 1, '2026-09-25', 52, 460, 83.2, 'Push session. Replaced standard dips with ring dips.'),
(4, 1, '2026-09-27', 62, 540, 83.0, 'Pull session + 15 min rowing cooldown.'),
(5, 2, '2026-09-17', 65, 510, 62.0, 'Heavy chest session. Incline bench was challenging.'),
(5, 2, '2026-09-19', 60, 480, 62.2, 'Back and bicep volume. Great pump on barbell rows.'),
(5, 2, '2026-09-22', 70, 560, 62.5, 'Heavy leg day. RDLs improved hamstring flexibility.'),
(5, 2, '2026-09-26', 60, 500, 62.7, 'Chest & triceps. Hit new PB on dumbbell presses.'),
(6, 3, '2026-09-18', 30, 340, 70.0, 'HIIT session 1 completed. Sweated intensely!'),
(6, 3, '2026-09-21', 35, 380, 69.8, 'Kettlebell swings felt easier, heart rate recovered faster.'),
(6, 3, '2026-09-25', 30, 360, 69.5, 'Completed 4 full rounds with minimal rest.');

-- Messages (Coach <-> User Interaction)
INSERT INTO messages (id, sender_id, receiver_id, message_text, sent_at, is_read) VALUES
(1, 4, 2, 'Hi Coach Marcus! I completed the Day 1 Lower body. My hamstrings are feeling the RDLs. Any tips on recovery?', '2026-09-16 10:15:00', 1),
(2, 2, 4, 'Hey John! Great work sticking to the plan. Make sure to hydrate with electrolytes and get 15 minutes of light walking or foam rolling today to promote blood flow.', '2026-09-16 11:30:00', 1),
(3, 4, 2, 'Thanks Coach! For Day 2 bench press, can I try adding 2.5kg to the last set?', '2026-09-18 09:00:00', 1),
(4, 2, 4, 'Absolutely! If your form remains solid on the first 3 sets, add the 2.5kg for the final set.', '2026-09-18 09:45:00', 1),
(5, 5, 2, 'Coach Marcus, my weight is trending up from 62.0 to 62.7kg as expected for hypertrophy. Muscle fullness is noticeable!', '2026-09-26 14:20:00', 0),
(6, 6, 3, 'Coach Elena, the HIIT Ignition intervals are intense! Can I split the 30 minutes into two 15-minute bouts?', '2026-09-20 16:00:00', 1),
(7, 3, 6, 'Hi Alex! Yes, doing 15 mins in the morning and 15 mins in the evening is an effective way to build endurance as a beginner!', '2026-09-20 17:10:00', 1);

-- User Feedback
INSERT INTO user_feedback (id, user_id, category, subject, message, rating, status) VALUES
(1, 4, 'Workout Plan', '12-Week Shred is fantastic', 'The exercises are well structured and progressive overload makes it exciting to hit the gym each week.', 5, 'REVIEWED'),
(2, 5, 'Coaching Interaction', 'Marcus is an exceptional mentor', 'Fast responses, actionable critique on lifting form, and great motivational push.', 5, 'REVIEWED'),
(3, 6, 'Platform Usability', 'Workout logging on mobile', 'Logging weights and sets is quick, but adding a rest timer chime would make it even more convenient!', 4, 'PENDING');

-- System Settings
INSERT INTO system_settings (setting_key, setting_value, setting_description) VALUES
('site_name', 'ApexFit Coaching Platform', 'Public platform name displayed in header and emails.'),
('contact_email', 'admin@apexfit.com', 'Primary administrative contact email.'),
('max_users_per_coach', '50', 'Maximum number of active users assigned per coach.'),
('allow_user_registration', 'true', 'Enable or disable open public registration for new users.'),
('maintenance_mode', 'false', 'Put site in maintenance mode for scheduled database updates.'),
('default_currency', 'USD', 'Default currency for subscription and coaching tiers.'),
('announcement', 'Welcome to ApexFit! Fall Season Fitness Challenge starts next Monday.', 'Global banner notification shown on all member dashboards.');
