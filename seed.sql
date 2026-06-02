-- seed.sql
--
-- Populates the AI Hardware Benchmark database with sample rows for development and testing.
-- This script should be executed after schema.sql has created all tables.

USE DBAIHardwareBenchmark;

-- Users
INSERT INTO User VALUES
('U001','Andrew','Vattuone','andrew@scu.edu','Researcher','2026-05-29 10:00:00'),
('U002','Ian','Tan','ian@scu.edu','Researcher','2026-05-29 10:05:00'),
('U003','Riley','Heike','riley@scu.edu','Researcher','2026-05-29 10:10:00');

-- Projects
INSERT INTO Project VALUES
('P001','Transformer Study',
 'Benchmarking transformer inference',
 'U001',
 '2026-05-02 09:00:00',
 'Active'),

('P002','CNN Benchmark',
 'Image classification hardware study',
 'U002',
 '2026-05-03 09:00:00',
 'Active');

-- AI Models
INSERT INTO AI_Model VALUES
('M001','BERT Large','Transformer',340000000,'FP32','HuggingFace',
 'Transformer inference model'),

('M002','GPT2 Medium','Transformer',355000000,'FP16','OpenAI',
 'Language model'),

('M003','ResNet50','CNN',25500000,'FP32','PyTorch',
 'Image classification model');

-- Workloads
INSERT INTO Workload VALUES
('W001','BERT Inference','Inference',
 'Transformer inference',
 'WikiText',
 32,
 512,
 NULL,
 NULL),

('W002','GPT Inference','Inference',
 'Language generation',
 'OpenWebText',
 16,
 1024,
 NULL,
 NULL),

('W003','Image Classification','Classification',
 'Image classification workload',
 'ImageNet',
 64,
 NULL,
 224,
 224);

-- Hardware Systems
INSERT INTO Hardware_System VALUES
('HS001','A100 Server','NVIDIA',
 'Xeon Gold',
 32,
 'A100',
 4,
 'GPU',
 512,
 'NVMe',
 'HBM2e',
 'A100 training server'),

('HS002','H100 Server','NVIDIA',
 'Xeon Platinum',
 64,
 'H100',
 4,
 'GPU',
 1024,
 'NVMe',
 'HBM3',
 'H100 benchmarking server'),

('HS003','RTX4090 Workstation','Custom',
 'Ryzen 7950X',
 16,
 'RTX4090',
 1,
 'GPU',
 128,
 'SSD',
 'GDDR6X',
 'Desktop benchmark system');

-- Hardware Configurations
INSERT INTO Hardware_Configuration VALUES
('HC001','HS001','A100 Full',
 512,2000,128,40,60,
 400,1410,'NVLink',
 'Full power'),

('HC002','HS002','H100 Full',
 1024,3000,128,50,80,
 350,1800,'NVLink',
 'Full power'),

('HC003','HS003','4090 Stock',
 128,1000,128,72,0,
 450,2520,'PCIe',
 'Default settings');

-- Software Environments
INSERT INTO Software_Environment VALUES
('SE001','Ubuntu','22.04',
 'PyTorch',
 '2.2',
 '12.1',
 '550.00',
 'GCC',
 '13.2',
 'Research environment'),

('SE002','Ubuntu','22.04',
 'TensorFlow',
 '2.15',
 '12.1',
 '550.00',
 'GCC',
 '13.2',
 'TensorFlow environment');

-- Optimization Methods
INSERT INTO Optimization_Method VALUES
('O001','Quantization','Model-level',
 'INT8',
 'INT8 quantization'),

('O002','Pruning','Model-level',
 'Structured',
 'Structured pruning'),

('O003','Distillation','Model-level',
 'Teacher-Student',
 'Knowledge distillation'),

('O004','Operator Fusion','Compiler-level',
 'Kernel Fusion',
 'Operator fusion');

-- Works_On
INSERT INTO Works_On VALUES
('U001','P001','Lead Researcher',
 '2026-05-02 10:00:00',
 'Project lead'),

('U002','P001','Research Assistant',
 '2026-05-02 10:00:00',
 'Works on benchmarking'),

('U003','P002','Lead Researcher',
 '2026-05-03 10:00:00',
 'CNN benchmark lead');

-- Experiment Runs
INSERT INTO Experiment_Run VALUES
('R001','P001','M001','W001',
 'HS001','HC001','SE001',
 'BERT_A100_INT8',
 '2026-05-10 10:00:00',
 'Completed',
 5,
 '1234',
 'Baseline BERT test',
 'U001'),

('R002','P001','M002','W002',
 'HS002','HC002','SE001',
 'GPT_H100_FP16',
 '2026-05-11 10:00:00',
 'Completed',
 5,
 '1235',
 'GPT benchmark',
 'U001'),

('R003','P002','M003','W003',
 'HS003','HC003','SE002',
 'ResNet_4090',
 '2026-05-12 10:00:00',
 'Completed',
 5,
 '1236',
 'CNN benchmark',
 'U003'),

('R004','P001','M001','W001',
 'HS002','HC002','SE001',
 'BERT_H100_INT8',
 '2026-05-13 10:00:00',
 'Completed',
 5,
 '1237',
 'BERT on H100',
 'U002');

-- Run Optimizations
INSERT INTO Experiment_Run_Optimization VALUES
('R001','O001',8,'INT8'),
('R002','O004',1,'Fusion'),
('R003','O002',20,'20 percent pruning'),
('R004','O001',8,'INT8');

INSERT INTO Experiment_Run_Optimization VALUES
('R004','O002',20,'20 percent pruning');

INSERT INTO Experiment_Run_Optimization VALUES
('R004','O003',1,'Knowledge distillation');

INSERT INTO Experiment_Run_Optimization VALUES
('R004','O004',1,'Operator fusion');

-- Benchmark Results
INSERT INTO Benchmark_Result VALUES
('BR001','R001','Average',1,'2026-05-10 11:00:00'),
('BR002','R002','Average',1,'2026-05-11 11:00:00'),
('BR003','R003','Average',1,'2026-05-12 11:00:00'),
('BR004','R004','Average',1,'2026-05-13 11:00:00');

-- Metrics
INSERT INTO Result_Metric VALUES
('BR001','Latency',1.40,'ms','Latency',FALSE),
('BR001','Throughput',9200,'tokens/sec','Throughput',TRUE),
('BR001','Power Consumption',310,'W','Power',FALSE),

('BR002','Latency',1.10,'ms','Latency',FALSE),
('BR002','Throughput',12000,'tokens/sec','Throughput',TRUE),
('BR002','Power Consumption',350,'W','Power',FALSE),

('BR003','Latency',3.20,'ms','Latency',FALSE),
('BR003','Throughput',5500,'images/sec','Throughput',TRUE),
('BR003','Power Consumption',180,'W','Power',FALSE),

('BR004','Latency',0.90,'ms','Latency',FALSE),
('BR004','Throughput',14000,'tokens/sec','Throughput',TRUE),
('BR004','Power Consumption',340,'W','Power',FALSE);