-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1:3306
-- Generation Time: Sep 26, 2025 at 02:13 PM
-- Server version: 9.3.0
-- PHP Version: 8.2.4

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `court_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `case_person`
--

CREATE TABLE `case_person` (
  `case_person_id` bigint UNSIGNED NOT NULL,
  `case_id` bigint UNSIGNED NOT NULL,
  `person_id` bigint UNSIGNED NOT NULL,
  `role` enum('PLAINTIFF','DEFENDANT','WITNESS','INTERESTED_PARTY','OTHER') NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- --------------------------------------------------------

--
-- Table structure for table `courthouse`
--

CREATE TABLE `courthouse` (
  `courthouse_id` bigint UNSIGNED NOT NULL,
  `name` varchar(200) NOT NULL,
  `location` varchar(200) DEFAULT NULL,
  `court_type` enum('High','District','Appeals','Other') NOT NULL DEFAULT 'Other',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- --------------------------------------------------------

--
-- Table structure for table `court_case`
--

CREATE TABLE `court_case` (
  `case_id` bigint UNSIGNED NOT NULL,
  `case_number` varchar(80) NOT NULL,
  `title` varchar(300) NOT NULL,
  `status` enum('FILED','ACTIVE','STAYED','CLOSED','APPEALED') NOT NULL DEFAULT 'FILED',
  `filed_date` date NOT NULL,
  `courthouse_id` bigint UNSIGNED NOT NULL,
  `assigned_judge_id` bigint UNSIGNED DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- --------------------------------------------------------

--
-- Table structure for table `hearing`
--

CREATE TABLE `hearing` (
  `hearing_id` bigint UNSIGNED NOT NULL,
  `case_id` bigint UNSIGNED NOT NULL,
  `scheduled_at` datetime NOT NULL,
  `room` varchar(80) DEFAULT NULL,
  `presiding_judge_id` bigint UNSIGNED DEFAULT NULL,
  `purpose` varchar(200) DEFAULT NULL,
  `outcome` varchar(300) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `judge`
--

CREATE TABLE `judge` (
  `judge_id` bigint UNSIGNED NOT NULL,
  `full_name` varchar(200) NOT NULL,
  `specialization` varchar(120) DEFAULT NULL,
  `courthouse_id` bigint UNSIGNED DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--

-- --------------------------------------------------------

--
-- Table structure for table `lawyer`
--

CREATE TABLE `lawyer` (
  `lawyer_id` bigint UNSIGNED NOT NULL,
  `full_name` varchar(200) NOT NULL,
  `firm` varchar(200) DEFAULT NULL,
  `license_no` varchar(60) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- --------------------------------------------------------

--
-- Table structure for table `person`
--

CREATE TABLE `person` (
  `person_id` bigint UNSIGNED NOT NULL,
  `full_name` varchar(200) NOT NULL,
  `email` varchar(160) DEFAULT NULL,
  `phone` varchar(40) DEFAULT NULL,
  `is_organization` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- --------------------------------------------------------

--
-- Table structure for table `representation`
--

CREATE TABLE `representation` (
  `representation_id` bigint UNSIGNED NOT NULL,
  `case_id` bigint UNSIGNED NOT NULL,
  `person_id` bigint UNSIGNED NOT NULL,
  `lawyer_id` bigint UNSIGNED DEFAULT NULL,
  `side` enum('PLAINTIFF','DEFENDANT','NEUTRAL') NOT NULL,
  `is_primary` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `user_account`
--

CREATE TABLE `user_account` (
  `user_id` bigint UNSIGNED NOT NULL,
  `full_name` varchar(200) NOT NULL,
  `email` varchar(160) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('ADMIN','CLERK','JUDGE','LAWYER','READONLY') NOT NULL DEFAULT 'CLERK',
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `case_person`
--
ALTER TABLE `case_person`
  ADD PRIMARY KEY (`case_person_id`),
  ADD UNIQUE KEY `case_id` (`case_id`,`person_id`,`role`),
  ADD KEY `idx_cp_case` (`case_id`),
  ADD KEY `idx_cp_person` (`person_id`);

--
-- Indexes for table `courthouse`
--
ALTER TABLE `courthouse`
  ADD PRIMARY KEY (`courthouse_id`);

--
-- Indexes for table `court_case`
--
ALTER TABLE `court_case`
  ADD PRIMARY KEY (`case_id`),
  ADD UNIQUE KEY `case_number` (`case_number`),
  ADD KEY `assigned_judge_id` (`assigned_judge_id`),
  ADD KEY `idx_case_status` (`status`),
  ADD KEY `idx_case_courthouse` (`courthouse_id`);

--
-- Indexes for table `hearing`
--
ALTER TABLE `hearing`
  ADD PRIMARY KEY (`hearing_id`),
  ADD KEY `presiding_judge_id` (`presiding_judge_id`),
  ADD KEY `idx_hearing_case_time` (`case_id`,`scheduled_at`);

--
-- Indexes for table `judge`
--
ALTER TABLE `judge`
  ADD PRIMARY KEY (`judge_id`),
  ADD KEY `courthouse_id` (`courthouse_id`);

--
-- Indexes for table `lawyer`
--
ALTER TABLE `lawyer`
  ADD PRIMARY KEY (`lawyer_id`),
  ADD UNIQUE KEY `license_no` (`license_no`);

--
-- Indexes for table `person`
--
ALTER TABLE `person`
  ADD PRIMARY KEY (`person_id`);

--
-- Indexes for table `representation`
--
ALTER TABLE `representation`
  ADD PRIMARY KEY (`representation_id`),
  ADD UNIQUE KEY `case_id` (`case_id`,`person_id`,`lawyer_id`,`side`),
  ADD KEY `idx_repr_case` (`case_id`),
  ADD KEY `idx_repr_person` (`person_id`),
  ADD KEY `idx_repr_lawyer` (`lawyer_id`);

--
-- Indexes for table `user_account`
--
ALTER TABLE `user_account`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `case_person`
--
ALTER TABLE `case_person`
  MODIFY `case_person_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `courthouse`
--
ALTER TABLE `courthouse`
  MODIFY `courthouse_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `court_case`
--
ALTER TABLE `court_case`
  MODIFY `case_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `hearing`
--
ALTER TABLE `hearing`
  MODIFY `hearing_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `judge`
--
ALTER TABLE `judge`
  MODIFY `judge_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `lawyer`
--
ALTER TABLE `lawyer`
  MODIFY `lawyer_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `person`
--
ALTER TABLE `person`
  MODIFY `person_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `representation`
--
ALTER TABLE `representation`
  MODIFY `representation_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `user_account`
--
ALTER TABLE `user_account`
  MODIFY `user_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `case_person`
--
ALTER TABLE `case_person`
  ADD CONSTRAINT `case_person_ibfk_1` FOREIGN KEY (`case_id`) REFERENCES `court_case` (`case_id`),
  ADD CONSTRAINT `case_person_ibfk_2` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`);

--
-- Constraints for table `court_case`
--
ALTER TABLE `court_case`
  ADD CONSTRAINT `court_case_ibfk_1` FOREIGN KEY (`courthouse_id`) REFERENCES `courthouse` (`courthouse_id`),
  ADD CONSTRAINT `court_case_ibfk_2` FOREIGN KEY (`assigned_judge_id`) REFERENCES `judge` (`judge_id`);

--
-- Constraints for table `hearing`
--
ALTER TABLE `hearing`
  ADD CONSTRAINT `hearing_ibfk_1` FOREIGN KEY (`case_id`) REFERENCES `court_case` (`case_id`),
  ADD CONSTRAINT `hearing_ibfk_2` FOREIGN KEY (`presiding_judge_id`) REFERENCES `judge` (`judge_id`);

--
-- Constraints for table `judge`
--
ALTER TABLE `judge`
  ADD CONSTRAINT `judge_ibfk_1` FOREIGN KEY (`courthouse_id`) REFERENCES `courthouse` (`courthouse_id`);

--
-- Constraints for table `representation`
--
ALTER TABLE `representation`
  ADD CONSTRAINT `representation_ibfk_1` FOREIGN KEY (`case_id`) REFERENCES `court_case` (`case_id`),
  ADD CONSTRAINT `representation_ibfk_2` FOREIGN KEY (`person_id`) REFERENCES `person` (`person_id`),
  ADD CONSTRAINT `representation_ibfk_3` FOREIGN KEY (`lawyer_id`) REFERENCES `lawyer` (`lawyer_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
