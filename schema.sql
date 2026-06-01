DROP DATABASE IF EXISTS DBAIHardwareBenchmark;
CREATE DATABASE DBAIHardwareBenchmark;
USE DBAIHardwareBenchmark;

CREATE TABLE User (
	user_id VARCHAR(20),
	first_name VARCHAR(20),
	last_name VARCHAR(20),
	email VARCHAR(20),
	role VARCHAR(20),
	created_at DATETIME,
	PRIMARY KEY (user_id)
);

CREATE TABLE Project (
	project_id VARCHAR(20),
	project_name VARCHAR(20),
	description VARCHAR(100),
	created_by VARCHAR(20),
	created_at DATETIME,
	status ENUM ('Active', 'Archived', 'Completed') NOT NULL,
	PRIMARY KEY (project_id),
	FOREIGN KEY (created_by) REFERENCES User (user_id) ON DELETE CASCADE
);

CREATE TABLE AI_Model (
	model_id VARCHAR(20),
	model_name VARCHAR(20),
	model_family ENUM ('CNN', 'Transformer', 'RNN', 'Diffusion Model') NOT NULL,
	parameter_count INTEGER,
	precision_default ENUM ('FP32', 'FP16', 'INT8') NOT NULL,
	source VARCHAR(20),
	description VARCHAR(100),
	PRIMARY KEY (model_id)
);

CREATE TABLE Workload (
	workload_id VARCHAR(20),
	workload_name VARCHAR(20),
	workload_type VARCHAR(20),
	input_description VARCHAR(100),
	dataset_name VARCHAR(20),
	batch_size INTEGER,
	sequence_length INTEGER,
	image_length INTEGER,
image_width INTEGER,
PRIMARY KEY (workload_id)
);

CREATE TABLE Hardware_System (
	system_id VARCHAR(20),
	system_name VARCHAR(20),
	vendor VARCHAR(20),
	cpu_model VARCHAR(20),
	cpu_core_count INTEGER,
	gpu_model VARCHAR(20),
	gpu_count INTEGER,
	accelerator_type VARCHAR(20),
	total_memory_gb INTEGER,
storage_type VARCHAR(20),
memory_type VARCHAR(20), 
description VARCHAR(100),
PRIMARY KEY (system_id)
);

CREATE TABLE Hardware_Configuration ( 
	config_id VARCHAR(20),
    	system_id VARCHAR(20),
   	config_name VARCHAR(20),
    	allocated_memory_gb INTEGER,
   	memory_bandwidth_gbps INTEGER,
    	cache_l1_kb INTEGER,
    	cache_l2_mb INTEGER,
   	cache_l3_mb INTEGER,
    	power_limit_watts INTEGER,
    	clock_speed_mhz INTEGER,
    	interconnect_type VARCHAR(20),
    	notes VARCHAR(100),

    	PRIMARY KEY (config_id, system_id),

    	FOREIGN KEY (system_id) REFERENCES Hardware_System(system_id) ON DELETE CASCADE
);

CREATE TABLE Software_Environment (
	environment_id VARCHAR(20),
	os_name VARCHAR(20),
	os_version VARCHAR(20),
	framework ENUM ('PyTorch', 'TensorFlow', 'ONNX Runtime', 'TensorRT') NOT NULL,
	framework_version VARCHAR(20),
	cuda_version VARCHAR(20),
	driver_version VARCHAR(20),
	compiler_name VARCHAR(20),
compiler_version VARCHAR(20),
library_notes VARCHAR(100),
PRIMARY KEY (environment_id)
);

CREATE TABLE Optimization_Method (
	optimization_id VARCHAR(20),
	optimization_name ENUM ('Quantization', 'Pruning', 'Distillation', 'Operator Fusion') NOT NULL,
	optimization_type ENUM ('Model-level', 'Runtime-level', 'Compiler-level') NOT NULL,
	optimization_subtype VARCHAR(30), 
	description VARCHAR(100),
PRIMARY KEY (optimization_id)
);

CREATE TABLE Experiment_Run (
	run_id VARCHAR(20),
	project_id VARCHAR(20),
	model_id VARCHAR(20),
	workload_id VARCHAR(20),
	system_id VARCHAR(20),
	config_id VARCHAR(20),
	environment_id VARCHAR(20),
	run_name VARCHAR(20),
	run_datetime DATETIME,
	run_status VARCHAR(20),
	num_trials INTEGER,
	random_seed VARCHAR(20),
	notes VARCHAR(100),
	created_by VARCHAR(20),
	PRIMARY KEY (run_id),
	FOREIGN KEY (project_id) REFERENCES Project(project_id) ON DELETE CASCADE,
FOREIGN KEY (model_id) REFERENCES AI_Model(model_id) ON DELETE CASCADE,
FOREIGN KEY (workload_id) REFERENCES Workload(workload_id) ON DELETE CASCADE,
FOREIGN KEY (config_id, system_id) REFERENCES Hardware_Configuration(config_id, system_id) ON DELETE CASCADE,
FOREIGN KEY (environment_id) REFERENCES Software_Environment(environment_id) ON DELETE CASCADE,
FOREIGN KEY (created_by) REFERENCES User(user_id) ON DELETE CASCADE
);

CREATE TABLE Benchmark_Result (
	result_id VARCHAR(20),
	run_id VARCHAR(20),
	aggregation_method ENUM ('Average', 'Median', 'P95', 'Max', 'Min') NOT NULL,
	trial_number INTEGER,
	recorded_at DATETIME,
PRIMARY KEY (result_id),
FOREIGN KEY (run_id) REFERENCES Experiment_Run(run_id) ON DELETE CASCADE
);

CREATE TABLE Result_Metric (
	result_id VARCHAR(20),
	metric_name ENUM ('Latency', 'Throughput', 'Power Consumption', 'Memory Usage', 'Energy Efficiency'),
metric_value DECIMAL(12, 4), 
	unit VARCHAR(20),
	description VARCHAR(100),
	higher_is_better BOOL,
PRIMARY KEY (result_id, metric_name),
FOREIGN KEY (result_id) REFERENCES Benchmark_Result(result_id) ON DELETE CASCADE
);

CREATE TABLE Works_On (
	user_id VARCHAR(20),
	project_id VARCHAR(20),
	member_role VARCHAR(20),
	join_date DATETIME,
	notes VARCHAR(100),
PRIMARY KEY (user_id, project_id),
FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE,
FOREIGN KEY (project_id) REFERENCES Project(project_id) ON DELETE CASCADE
);

CREATE TABLE Experiment_Run_Optimization (
	run_id VARCHAR(20),
optimization_id VARCHAR(20),
	optimization_value INTEGER,
	notes VARCHAR(100),
PRIMARY KEY (run_id, optimization_id),
FOREIGN KEY (run_id) REFERENCES Experiment_Run(run_id) ON DELETE CASCADE,
FOREIGN KEY (optimization_id) REFERENCES Optimization_Method (optimization_id) ON DELETE CASCADE
);