-- ============================================================
-- REAL DATA from OpenAlex API (SQL Server Version)
-- Adapted for the current SWP391 schema
-- ============================================================

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;

USE PublicationTracker;

-- Note: Truncation/constraint checks are omitted as the tables
-- are freshly recreated and empty from schema_mssql.sql execution.

-- ============================================================
-- RESEARCH FIELDS
-- ============================================================
INSERT INTO research_fields (field_name, description) VALUES
    ('Computer Science', 'Research related to AI, machine learning, computer vision, NLP, and data mining.'),
    ('Bioinformatics', 'Research related to computational biology, molecular evolution, and biomedical analysis.');

-- ============================================================
-- JOURNALS
-- ============================================================
INSERT INTO journals (name, issn, publisher, status) VALUES
    ('Lecture Notes in Computer Science', '', 'Springer', 'active'),
    ('Nature', '', 'Springer Nature', 'active'),
    ('DROPS (Schloss Dagstuhl - Leibniz Center for Informatics)', '', 'Schloss Dagstuhl', 'active'),
    ('Communications of the ACM', '', 'ACM', 'active'),
    ('arXiv (Cornell University)', '', 'Cornell University', 'active'),
    ('IEEE Transactions on Pattern Analysis and Machine Intelligence', '', 'IEEE', 'active'),
    ('Molecular Biology and Evolution', '', 'Oxford University Press', 'active'),
    ('International Journal of Computer Vision', '', 'Springer', 'active'),
    ('Bioinformatics', '', 'Oxford University Press', 'active');

-- ============================================================
-- AUTHORS
-- ============================================================
INSERT INTO authors (full_name, affiliation, orcid) VALUES
    ('Kathryn Tunyasuvunakool', 'Google DeepMind (United Kingdom)', NULL),
    ('Alexander Pritzel', 'Google DeepMind (United Kingdom)', NULL),
    ('Jonathan Krause', 'Stanford University', NULL),
    ('John Jumper', 'Google DeepMind (United Kingdom)', NULL),
    ('Ilya Sutskever', 'University of Toronto', NULL),
    ('Serge Belongie', 'Cornell University', NULL),
    ('Scott Reed', 'University of Michigan - Ann Arbor', NULL),
    ('Russ Bates', 'Google DeepMind (United Kingdom)', NULL),
    ('Geoffrey E. Hinton', 'Google (United States)', NULL),
    ('Koichiro Tamura', 'Tokyo Metropolitan University', NULL),
    ('Aditya Khosla', 'Massachusetts Institute of Technology', NULL),
    ('Jian Sun', 'Microsoft Research (United Kingdom)', NULL),
    ('Piotr Dollar', 'Microsoft (United States)', NULL),
    ('Jonathan Long', 'University of California, Berkeley', NULL),
    ('Nitish Srivastava', 'University of Toronto', NULL),
    ('Michalina Pacholska', 'Google DeepMind (United Kingdom)', NULL),
    ('James Hays', 'John Brown University', NULL),
    ('Yoshua Bengio', 'Universite de Montreal', NULL),
    ('Vincent Vanhoucke', 'IGlobal University', NULL),
    ('Tim Green', 'Google DeepMind (United Kingdom)', NULL),
    ('Hao Su', 'Stanford University', NULL),
    ('Michael S. Bernstein', 'Stanford University', NULL),
    ('Andrej Karpathy', 'Stanford University', NULL),
    ('Pushmeet Kohli', 'Google DeepMind (United Kingdom)', NULL),
    ('Michael Maire', 'California Institute of Technology', NULL),
    ('Alexandros Stamatakis', 'Karlsruhe Institute of Technology', NULL),
    ('Zhiheng Huang', 'Stanford University', NULL),
    ('Christopher D. Manning', 'Stanford University', NULL),
    ('Ruslan Salakhutdinov', 'University of Toronto', NULL),
    ('Andrew J. Ballard', 'Google DeepMind (United Kingdom)', NULL),
    ('Augustin Zidek', 'Google DeepMind (United Kingdom)', NULL),
    ('Sudhir Kumar', 'King Abdulaziz University', NULL),
    ('Daniel S. Peterson', 'Arizona State University', NULL),
    ('Philipp Fischer', 'University of Freiburg', NULL),
    ('Gao Huang', 'Cornell University', NULL),
    ('Anna Potapenko', 'Google DeepMind (United Kingdom)', NULL),
    ('Zhuang Liu', 'Tsinghua University', NULL),
    ('Laurens van der Maaten', 'Meta (Israel)', NULL),
    ('David Reiman', 'Google DeepMind (United Kingdom)', NULL),
    ('Karen Simonyan', 'University of Oxford', NULL),
    ('Ross Girshick', 'Meta (United States)', NULL),
    ('Andrew Zisserman', 'University of Oxford', NULL),
    ('Dumitru Erhan', 'Google (United States)', NULL),
    ('Alex Krizhevsky', 'University of Toronto', NULL),
    ('Tamas Berghammer', 'Google DeepMind (United Kingdom)', NULL),
    ('Deva Ramanan', 'UC Irvine Health', NULL),
    ('Oriol Vinyals', 'Google DeepMind (United Kingdom)', NULL),
    ('Wei Liu', 'University of North Carolina at Chapel Hill', NULL),
    ('Thomas Brox', 'University of Freiburg', NULL),
    ('Glen Stecher', 'Arizona State University', NULL),
    ('Andrew Rabinovich', 'Magic Leap (United States)', NULL),
    ('Demis Hassabis', 'Google DeepMind (United Kingdom)', NULL),
    ('Clemens Meyer', 'Google DeepMind (United Kingdom)', NULL),
    ('Jeffrey Pennington', 'Stanford University', NULL),
    ('Jia Deng', 'University of Michigan - Ann Arbor', NULL),
    ('Xiangyu Zhang', 'Microsoft Research (United Kingdom)', NULL),
    ('Ellen Clancy', 'Google DeepMind (United Kingdom)', NULL),
    ('Rishub Jain', 'Google DeepMind (United Kingdom)', NULL),
    ('Sanjeev Satheesh', 'Stanford University', NULL),
    ('Michael Figurnov', 'Google DeepMind (United Kingdom)', NULL),
    ('Alan Filipski', 'Arizona State University', NULL),
    ('Pierre Sermanet', 'IGlobal University', NULL),
    ('Pietro Perona', 'California Institute of Technology', NULL),
    ('Sean Ma', 'Stanford University', NULL),
    ('Olga Russakovsky', 'Stanford University', NULL),
    ('Alexander C. Berg', 'University of North Carolina at Chapel Hill', NULL),
    ('Tsung-Yi Lin', 'Cornell University', NULL),
    ('Jian Sun', 'Microsoft (United States)', NULL),
    ('Jonas Adler', 'Google DeepMind (United Kingdom)', NULL),
    ('Dragomir Anguelov', 'Google (United States)', NULL),
    ('Alex Bridgland', 'Google DeepMind (United Kingdom)', NULL),
    ('Evan Shelhamer', 'University of California, Berkeley', NULL),
    ('Koray Kavukcuoglu', 'Google DeepMind (United Kingdom)', NULL),
    ('Trevor Darrell', 'Berkeley College', NULL),
    ('Andrew Cowie', 'Google DeepMind (United Kingdom)', NULL),
    ('Stig Petersen', 'Google DeepMind (United Kingdom)', NULL),
    ('Kilian Q. Weinberger', 'Cornell University', NULL),
    ('Stanislav Nikolov', 'Google DeepMind (United Kingdom)', NULL),
    ('Christian Szegedy', 'Google (United States)', NULL),
    ('Simon Kohl', 'Google DeepMind (United Kingdom)', NULL),
    ('Kaiming He', 'Microsoft Research (United Kingdom)', NULL),
    ('Richard Socher', '', NULL),
    ('C. Lawrence Zitnick', 'Microsoft (United States)', NULL),
    ('Sebastian W. Bodenstein', 'Google DeepMind (United Kingdom)', NULL),
    ('Yann LeCun', 'Meta (United States)', NULL),
    ('Bernardino Romera-Paredes', 'Google DeepMind (United Kingdom)', NULL),
    ('Shaoqing Ren', 'Microsoft Research (United Kingdom)', NULL),
    ('David Silver', 'Google DeepMind (United Kingdom)', NULL),
    ('Michal Zielinski', 'Google DeepMind (United Kingdom)', NULL),
    ('Yangqing Jia', 'IGlobal University', NULL),
    ('Andrew F. Hayes', 'The Ohio State University', NULL),
    ('Olaf Ronneberger', 'University of Freiburg', NULL),
    ('Trevor Back', 'Google DeepMind (United Kingdom)', NULL),
    ('Andrew Senior', 'Google DeepMind (United Kingdom)', NULL),
    ('Martin Steinegger', 'Seoul National University', NULL),
    ('Li Fei-Fei', 'Stanford University', NULL);

-- ============================================================
-- KEYWORDS
-- ============================================================
INSERT INTO keywords (keyword_name) VALUES
    ('deep learning'),
    ('computer vision'),
    ('transformer'),
    ('image recognition'),
    ('object detection'),
    ('protein structure prediction'),
    ('bioinformatics'),
    ('optimization'),
    ('word embedding'),
    ('semantic segmentation'),
    ('evolutionary genetics'),
    ('benchmark dataset');

-- ============================================================
-- PAPERS
-- ============================================================
INSERT INTO papers (
    journal_id,
    field_id,
    api_source_id,
    publication_type,
    title,
    abstract,
    publication_year,
    doi,
    source_url,
    citation_count,
    visibility_status
) VALUES
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'Deep Residual Learning for Image Recognition', NULL, 2016, '10.1109/cvpr.2016.90', 'https://doi.org/10.1109/cvpr.2016.90', 220052, 'VISIBLE'),
    (NULL, 1, 1, 'BOOK_CHAPTER', 'U-Net: Convolutional Networks for Biomedical Image Segmentation', NULL, 2015, '10.1007/978-3-319-24574-4_28', 'https://doi.org/10.1007/978-3-319-24574-4_28', 87895, 'VISIBLE'),
    (2, 1, 1, 'JOURNAL_ARTICLE', 'Deep learning', NULL, 2015, '10.1038/nature14539', 'https://doi.org/10.1038/nature14539', 80583, 'VISIBLE'),
    (NULL, 1, 1, 'REPOSITORY_ITEM', 'MizAR 60 for Mizar 50', NULL, 2023, '10.4230/lipics.itp.2023.19', 'https://drops.dagstuhl.de/entities/document/10.4230/LIPIcs.ITP.2023.19', 75682, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'ImageNet classification with deep convolutional neural networks', NULL, 2017, '10.1145/3065386', 'https://doi.org/10.1145/3065386', 75677, 'VISIBLE'),
    (NULL, 1, 1, 'PREPRINT', 'Very Deep Convolutional Networks for Large-Scale Image Recognition', NULL, 2014, '10.48550/arxiv.1409.1556', 'http://arxiv.org/abs/1409.1556', 75505, 'VISIBLE'),
    (6, 1, 1, 'JOURNAL_ARTICLE', 'Faster R-CNN: Towards Real-Time Object Detection with Region Proposal Networks', NULL, 2016, '10.1109/tpami.2016.2577031', 'https://doi.org/10.1109/tpami.2016.2577031', 53659, 'VISIBLE'),
    (7, 2, 1, 'JOURNAL_ARTICLE', 'MEGA6: Molecular Evolutionary Genetics Analysis Version 6.0', NULL, 2013, '10.1093/molbev/mst197', 'https://doi.org/10.1093/molbev/mst197', 47813, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'XGBoost', NULL, 2016, '10.1145/2939672.2939785', 'https://doi.org/10.1145/2939672.2939785', 47080, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'Going deeper with convolutions', NULL, 2015, '10.1109/cvpr.2015.7298594', 'https://doi.org/10.1109/cvpr.2015.7298594', 46648, 'VISIBLE'),
    (NULL, 1, 1, 'REPOSITORY_ITEM', 'AI-Assisted Pipeline for Dynamic Generation of Trustworthy Health Supplement Content at Scale', NULL, 2018, '10.4230/lipics.cosit.2022.18', 'https://drops.dagstuhl.de/entities/document/10.4230/OASIcs.LDK.2019.21', 45559, 'VISIBLE'),
    (NULL, 1, 1, 'OTHER', 'Introduction to Mediation, Moderation, and Conditional Process Analysis: A Regression-Based Approach', NULL, 2013, NULL, 'http://bvbr.bib-bvb.de:8991/F?func=service&doc_library=BVB01&local_base=BVB01&doc_number=025778167&sequence=000003&line_number=0001&func_code=DB_RECORDS&service_type=MEDIA', 45131, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'Densely Connected Convolutional Networks', NULL, 2017, '10.1109/cvpr.2017.243', 'https://doi.org/10.1109/cvpr.2017.243', 44228, 'VISIBLE'),
    (2, 2, 1, 'JOURNAL_ARTICLE', 'Highly accurate protein structure prediction with AlphaFold', NULL, 2021, '10.1038/s41586-021-03819-2', 'https://doi.org/10.1038/s41586-021-03819-2', 44128, 'VISIBLE'),
    (NULL, 1, 1, 'BOOK_CHAPTER', 'Microsoft COCO: Common Objects in Context', NULL, 2014, '10.1007/978-3-319-10602-1_48', 'https://doi.org/10.1007/978-3-319-10602-1_48', 41868, 'VISIBLE'),
    (8, 1, 1, 'JOURNAL_ARTICLE', 'ImageNet Large Scale Visual Recognition Challenge', NULL, 2015, '10.1007/s11263-015-0816-y', 'https://doi.org/10.1007/s11263-015-0816-y', 40009, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'Fully convolutional networks for semantic segmentation', NULL, 2015, '10.1109/cvpr.2015.7298965', 'https://doi.org/10.1109/cvpr.2015.7298965', 36709, 'VISIBLE'),
    (NULL, 1, 1, 'OTHER', 'Dropout: a simple way to prevent neural networks from overfitting', NULL, 2014, NULL, 'http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.669.8604', 34247, 'VISIBLE'),
    (9, 2, 1, 'JOURNAL_ARTICLE', 'RAxML version 8: a tool for phylogenetic analysis and post-analysis of large phylogenies', NULL, 2014, '10.1093/bioinformatics/btu033', 'https://doi.org/10.1093/bioinformatics/btu033', 34084, 'VISIBLE'),
    (NULL, 1, 1, 'CONFERENCE_PAPER', 'GloVe: Global Vectors for Word Representation', NULL, 2014, '10.3115/v1/d14-1162', 'https://doi.org/10.3115/v1/d14-1162', 33579, 'VISIBLE');

-- ============================================================
-- PAPER AUTHORS
-- ============================================================
INSERT INTO paper_authors (paper_id, author_id, author_order) VALUES
    (1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 4, 4),
    (2, 5, 1), (2, 6, 2), (2, 7, 3),
    (3, 8, 1), (3, 9, 2), (3, 10, 3),
    (5, 11, 1), (5, 12, 2), (5, 10, 3),
    (6, 13, 1), (6, 14, 2),
    (7, 3, 1), (7, 1, 2), (7, 15, 3), (7, 16, 4),
    (8, 17, 1), (8, 18, 2), (8, 19, 3), (8, 20, 4), (8, 21, 5),
    (10, 22, 1), (10, 23, 2), (10, 24, 3), (10, 25, 4), (10, 26, 5),
    (12, 31, 1),
    (13, 32, 1), (13, 33, 2), (13, 34, 3), (13, 35, 4),
    (14, 36, 1), (14, 37, 2), (14, 38, 3), (14, 39, 4), (14, 5, 5),
    (15, 68, 1), (15, 69, 2), (15, 70, 3), (15, 71, 4), (15, 72, 5),
    (16, 76, 1), (16, 77, 2), (16, 78, 3), (16, 79, 4), (16, 80, 5),
    (17, 88, 1), (17, 89, 2), (17, 90, 3),
    (18, 91, 1), (18, 10, 2), (18, 11, 3), (18, 12, 4), (18, 92, 5),
    (19, 93, 1),
    (20, 94, 1), (20, 95, 2), (20, 96, 3);

-- ============================================================
-- PAPER KEYWORDS
-- ============================================================
INSERT INTO paper_keywords (paper_id, keyword_id) VALUES
    (1, 1), (1, 4),
    (2, 10), (2, 7),
    (3, 1),
    (5, 1), (5, 4), (5, 12),
    (6, 1), (6, 4),
    (7, 5), (7, 4),
    (8, 7), (8, 11),
    (9, 8),
    (10, 1), (10, 4),
    (13, 1), (13, 4),
    (14, 6), (14, 7),
    (15, 12), (15, 4),
    (16, 4), (16, 12),
    (17, 10), (17, 4),
    (18, 8), (18, 1),
    (19, 7), (19, 11),
    (20, 9);

-- ============================================================
-- TOPICS
-- ============================================================
INSERT INTO topics (topic_name, description) VALUES
    ('Artificial Intelligence', 'Research related to machine learning, neural networks, and reasoning.'),
    ('Computer Vision', 'Research related to image classification, object detection, and segmentation.'),
    ('Natural Language Processing', 'Research related to text classification, translation, and language modeling.'),
    ('Bioinformatics', 'Research related to sequence analysis and biological computing.');

-- ============================================================
-- PAPER TOPICS
-- ============================================================
INSERT INTO paper_topics (paper_id, topic_id) VALUES
    (1, 1), (1, 2),
    (2, 2), (2, 4),
    (3, 1),
    (4, 1),
    (5, 1), (5, 2),
    (6, 1),
    (7, 1),
    (8, 1),
    (9, 1),
    (10, 1),
    (11, 2),
    (12, 1),
    (13, 1), (13, 2),
    (14, 4),
    (15, 2),
    (16, 2),
    (17, 2),
    (18, 1),
    (19, 4),
    (20, 3);

-- ============================================================
-- ABSTRACT UPDATES FROM OPENALEX / PUBLIC SOURCES
-- ============================================================
UPDATE papers SET abstract = 'Deeper neural networks are more difficult to train. We present a residual learning framework to ease the training of networks that are substantially deeper than those used previously. We explicitly reformulate the layers as learning residual functions with reference to the layer inputs, instead of learning unreferenced functions. We provide comprehensive empirical evidence showing that these residual networks are easier to optimize, and can gain accuracy from considerably increased depth. On the ImageNet dataset we evaluate residual nets with a depth of up to 152 layers - 8x deeper than VGG nets but still having lower complexity. An ensemble of these residual nets achieves 3.57% error on the ImageNet test set. This result won the 1st place on the ILSVRC 2015 classification task. We also present analysis on CIFAR-10 with 100 and 1000 layers. The depth of representations is of central importance for many visual recognition tasks. Solely due to our extremely deep representations, we obtain a 28% relative improvement on the COCO object detection dataset.' WHERE paper_id = 1;
UPDATE papers SET abstract = 'There is large consent that successful training of deep networks requires many thousand annotated training samples. In this paper, we present a network and training strategy that relies on the strong use of data augmentation to use the available annotated samples more efficiently. The architecture consists of a contracting path to capture context and a symmetric expanding path that enables precise localization. We show that such a network can be trained end-to-end from very few images and outperforms the prior best method on the ISBI challenge for segmentation of neuronal structures in electron microscopic stacks. Using the same network trained on transmitted light microscopy images we won the ISBI cell tracking challenge 2015 by a large margin. Moreover, the network is fast. Segmentation of a 512x512 image takes less than a second on a recent GPU.' WHERE paper_id = 2;
UPDATE papers SET abstract = 'Deep learning allows computational models that are composed of multiple processing layers to learn representations of data with multiple levels of abstraction. These methods have dramatically improved the state of the art in speech recognition, visual object recognition, object detection and many other domains such as drug discovery and genomics. Deep learning discovers intricate structure in large data sets by using backpropagation to indicate how a machine should change its internal parameters that are used to compute the representation in each layer from the representation in the previous layer. Deep convolutional nets have brought about breakthroughs in processing images, video, speech and audio, whereas recurrent nets have shone light on sequential data such as text and speech.' WHERE paper_id = 3;
UPDATE papers SET abstract = 'As a present to Mizar on its 50th anniversary, we develop an AI and theorem proving system that automatically proves about 60 percent of the Mizar theorems in the hammer setting. We also automatically prove 75 percent of the Mizar theorems when the automated provers are helped by using only the premises used in the human written Mizar proofs. We describe the methods and large scale experiments leading to these results. This includes in particular the E and Vampire provers, their ENIGMA and Deepire learning modifications, a number of learning based premise selection methods, and the incremental loop that interleaves growing a corpus of millions of ATP proofs with training increasingly strong AI and theorem proving systems on them.' WHERE paper_id = 4;
UPDATE papers SET abstract = 'We trained a large, deep convolutional neural network to classify the 1.2 million high resolution images in the ImageNet contest into 1000 different classes. On the test data, we achieved top-1 and top-5 error rates of 37.5 percent and 17.0 percent, which is considerably better than the previous state of the art. The neural network, which has 60 million parameters and 650,000 neurons, consists of five convolutional layers and three fully connected layers with a final 1000-way softmax. To make training faster, we used non saturating neurons and a very efficient GPU implementation of the convolution operation. To reduce overfitting in the fully connected layers we employed dropout.' WHERE paper_id = 5;
UPDATE papers SET abstract = 'In this work we investigate the effect of the convolutional network depth on its accuracy in the large scale image recognition setting. Our main contribution is a thorough evaluation of networks of increasing depth using an architecture with very small 3x3 convolution filters, which shows that a significant improvement on prior configurations can be achieved by pushing the depth to 16 to 19 weight layers. These findings were the basis of our ImageNet Challenge 2014 submission, where our team secured the first and second places in the localisation and classification tracks respectively. We also show that our representations generalise well to other datasets, where they achieve state of the art results.' WHERE paper_id = 6;
UPDATE papers SET abstract = 'State of the art object detection networks depend on region proposal algorithms to hypothesize object locations. Advances like SPPnet and Fast R-CNN have reduced the running time of these detection networks, exposing region proposal computation as a bottleneck. In this work, we introduce a Region Proposal Network that shares full image convolutional features with the detection network, thus enabling nearly cost free region proposals. An RPN is a fully convolutional network that simultaneously predicts object bounds and objectness scores at each position. We further merge RPN and Fast R-CNN into a single network by sharing their convolutional features.' WHERE paper_id = 7;
UPDATE papers SET abstract = 'The Molecular Evolutionary Genetics Analysis software has matured to contain a large collection of methods and tools of computational molecular evolution. Here, we describe new additions that make MEGA a more comprehensive tool for building timetrees of species, pathogens, and gene families using rapid relaxed clock methods. Methods for estimating divergence times and confidence intervals are implemented to use probability densities for calibration constraints for node dating and sequence sampling dates for tip dating analyses. These enhancements improve the user experience, quality of results, and the pace of biological discovery.' WHERE paper_id = 8;
UPDATE papers SET abstract = 'Tree boosting is a highly effective and widely used machine learning method. In this paper, we describe a scalable end to end tree boosting system called XGBoost, which is used widely by data scientists to achieve state of the art results on many machine learning challenges. We propose a novel sparsity aware algorithm for sparse data and weighted quantile sketch for approximate tree learning. More importantly, we provide insights on cache access patterns, data compression and sharding to build a scalable tree boosting system. By combining these insights, XGBoost scales beyond billions of examples using far fewer resources than existing systems.' WHERE paper_id = 9;
UPDATE papers SET abstract = 'We propose a deep convolutional neural network architecture codenamed Inception that achieves the new state of the art for classification and detection in the ImageNet Large Scale Visual Recognition Challenge 2014. The hallmark of this architecture is the improved utilization of the computing resources inside the network. By a carefully crafted design, we increased the depth and width of the network while keeping the computational budget constant. One particular incarnation used in our submission for ILSVRC14 is called GoogLeNet, a 22 layer deep network whose quality is assessed in the context of classification and detection.' WHERE paper_id = 10;
UPDATE papers SET abstract = 'Although geospatial question answering systems have received increasing attention in recent years, existing prototype systems struggle to properly answer qualitative spatial questions. In this work, we propose a framework for answering qualitative spatial questions, comprising a geoparser that extracts place semantic information from text, a reasoning system with a crisp reasoner, and answer extraction that refines the solution space and generates final answers. We present an experimental design to evaluate the framework for point based cardinal direction calculus relations using synthetic qualitative spatial questions.' WHERE paper_id = 11;
UPDATE papers SET abstract = 'This work presents the concepts and methods behind mediation, moderation, and conditional process analysis using a regression based approach. It introduces simple and multiple regression, direct and indirect effects, moderated relationships, conditional effects, and practical guidance for analyzing and reporting conditional process models with statistical software.' WHERE paper_id = 12;
UPDATE papers SET abstract = 'Recent work has shown that convolutional networks can be substantially deeper, more accurate, and efficient to train if they contain shorter connections between layers close to the input and those close to the output. In this paper, we introduce the Dense Convolutional Network, which connects each layer to every other layer in a feed forward fashion. DenseNets alleviate the vanishing gradient problem, strengthen feature propagation, encourage feature reuse, and substantially reduce the number of parameters. We evaluate the architecture on CIFAR-10, CIFAR-100, SVHN, and ImageNet.' WHERE paper_id = 13;
UPDATE papers SET abstract = 'Proteins are essential to life, and understanding their structure can facilitate a mechanistic understanding of their function. Through an enormous experimental effort, the structures of around 100,000 unique proteins have been determined, but this represents a small fraction of the billions of known protein sequences. Here we provide a computational method that can regularly predict protein structures with atomic accuracy even in cases in which no similar structure is known. We validated a redesigned version of AlphaFold in CASP14, demonstrating accuracy competitive with experimental structures in a majority of cases and greatly outperforming other methods.' WHERE paper_id = 14;
UPDATE papers SET abstract = 'We present a dataset with the goal of advancing the state of the art in object recognition by placing the question of object recognition in the broader context of scene understanding. This is achieved by gathering images of complex everyday scenes containing common objects in their natural context. Objects are labeled using per instance segmentations to aid in precise object localization. The dataset contains 91 object types with 2.5 million labeled instances in 328 thousand images. We provide baseline performance analysis for bounding box and segmentation detection results using a Deformable Parts Model.' WHERE paper_id = 15;
UPDATE papers SET abstract = 'The ImageNet Large Scale Visual Recognition Challenge is a benchmark in object category classification and detection on hundreds of object categories and millions of images. The challenge has been run annually from 2010 onward, attracting participation from more than fifty institutions. This paper describes the creation of this benchmark dataset and the advances in object recognition that have been possible as a result. We discuss the challenges of collecting large scale ground truth annotation, highlight key breakthroughs in categorical object recognition, and compare computer vision accuracy with human accuracy.' WHERE paper_id = 16;
UPDATE papers SET abstract = 'Convolutional networks are powerful visual models that yield hierarchies of features. We show that convolutional networks by themselves, trained end to end and pixels to pixels, exceed the state of the art in semantic segmentation. Our key insight is to build fully convolutional networks that take input of arbitrary size and produce correspondingly sized output with efficient inference and learning. We adapt contemporary classification networks into fully convolutional networks and define a skip architecture that combines semantic information from a deep coarse layer with appearance information from a shallow fine layer to produce accurate and detailed segmentations.' WHERE paper_id = 17;
UPDATE papers SET abstract = 'Deep neural nets with a large number of parameters are very powerful machine learning systems. However, overfitting is a serious problem in such networks. Dropout is a technique for addressing this problem by randomly dropping units along with their connections from the neural network during training. This prevents units from co adapting too much. At test time, it is easy to approximate the effect of averaging the predictions of many thinned networks by using a single unthinned network that has smaller weights. This significantly reduces overfitting and gives major improvements over other regularization methods.' WHERE paper_id = 18;
UPDATE papers SET abstract = 'Phylogenies are increasingly used in all fields of medical and biological research. Because of next generation sequencing, datasets used for phylogenetic analyses grow at an unprecedented pace. RAxML is a popular program for phylogenetic analyses of large datasets under maximum likelihood. Since the last RAxML paper in 2006, it has been continuously maintained and extended to accommodate growing input datasets and to serve the needs of the user community. The work describes notable new features and extensions of RAxML, including support for more data types, improved memory usage, and post analysis operations on sets of trees.' WHERE paper_id = 19;
UPDATE papers SET abstract = 'Recent methods for learning vector space representations of words have succeeded in capturing fine grained semantic and syntactic regularities using vector arithmetic, but the origin of these regularities has remained opaque. We analyze and make explicit the model properties needed for such regularities to emerge in word vectors. The result is a new global log bilinear regression model that combines the advantages of global matrix factorization and local context window methods. The model efficiently leverages statistical information by training only on the nonzero elements in a word word co occurrence matrix.' WHERE paper_id = 20;

-- ============================================================
-- DATA CONSISTENCY CLEANUP
-- journal_id should be present only for journal_article
-- ============================================================
UPDATE papers
SET journal_id = NULL
WHERE publication_type IN (
    'CONFERENCE_PAPER',
    'PREPRINT',
    'BOOK_CHAPTER',
    'REPOSITORY_ITEM',
    'OTHER'
);

-- ============================================================
-- SAMPLE SUBSCRIPTIONS, ORDERS, TRANSACTIONS & FOLLOWS
-- ============================================================
DECLARE @member_user_id BIGINT;
SELECT @member_user_id = user_id FROM users WHERE email = 'member@gmail.com';

DECLARE @monthly_plan_id INT;
SELECT @monthly_plan_id = plan_id FROM subscription_plans WHERE plan_name = 'PREMIUM_MONTHLY';

DECLARE @monthly_price DECIMAL(10, 2);
SELECT @monthly_price = price FROM subscription_plans WHERE plan_name = 'PREMIUM_MONTHLY';

IF @member_user_id IS NOT NULL AND @monthly_plan_id IS NOT NULL
BEGIN
    -- 1. Insert a sample active subscription for member@gmail.com
    INSERT INTO premium_subscriptions (user_id, plan_id, subscribed_price, discount, start_date, end_date, status)
    VALUES (@member_user_id, @monthly_plan_id, ISNULL(@monthly_price, 49000.00), 0.00, CAST(GETDATE() - 1 AS DATE), CAST(GETDATE() + 29 AS DATE), 'ACTIVE');

    -- 2. Insert a sample order record
    INSERT INTO premium_orders (user_id, plan_id, amount, discount, status, created_at, updated_at)
    VALUES (@member_user_id, @monthly_plan_id, ISNULL(@monthly_price, 49000.00), 0.00, 'PAID', GETDATE() - 1, GETDATE() - 1);

    -- Get the inserted order_id
    DECLARE @order_id BIGINT;
    SELECT @order_id = SCOPE_IDENTITY();

    -- 3. Insert a sample payment transaction record referencing the order
    INSERT INTO payment_transactions (user_id, plan_id, order_id, amount, transaction_type, payment_method, status, transaction_date)
    VALUES (@member_user_id, @monthly_plan_id, @order_id, ISNULL(@monthly_price, 49000.00), 'BUY', 'Credit Card', 'SUCCESS', GETDATE() - 1);

    -- 4. Insert follow records for testing
    DECLARE @author_hinton_id BIGINT;
    SELECT @author_hinton_id = author_id FROM authors WHERE full_name = 'Geoffrey E. Hinton';
    IF @author_hinton_id IS NOT NULL
    BEGIN
        INSERT INTO follow_author (user_id, author_id)
        VALUES (@member_user_id, @author_hinton_id);
    END

    DECLARE @first_topic_id INT;
    SELECT TOP 1 @first_topic_id = topic_id FROM topics;
    IF @first_topic_id IS NOT NULL
    BEGIN
        INSERT INTO follow_topic (user_id, topic_id)
        VALUES (@member_user_id, @first_topic_id);
    END

    -- 5. Insert realistic notifications for follow, login, and payment success
    INSERT INTO notifications (user_id, type, title, content, is_read, created_at)
    VALUES 
    (@member_user_id, 'LOGIN_SUCCESS', 'Đăng nhập thành công', 'Chào mừng bạn quay trở lại! Bạn đã đăng nhập thành công vào hệ thống bằng JWT.', 0, DATEADD(minute, -10, GETDATE())),
    (@member_user_id, 'PAYMENT_SUCCESS', 'Thanh toán thành công gói PREMIUM_MONTHLY', CONCAT('Cảm ơn bạn đã đăng ký gói PREMIUM_MONTHLY (', CAST(ISNULL(@monthly_price, 49000.00) AS VARCHAR(20)), 'đ). Gói dịch vụ Premium đã được kích hoạt thành công.', ''), 0, DATEADD(hour, -24, GETDATE())),
    (@member_user_id, 'FOLLOW_AUTHOR', 'Đã theo dõi tác giả', 'Bạn đã bắt đầu theo dõi tác giả Geoffrey E. Hinton. Bạn sẽ nhận được thông báo khi có bài viết mới của tác giả này.', 0, DATEADD(minute, -5, GETDATE())),
    (@member_user_id, 'FOLLOW_TOPIC', 'Đã theo dõi chủ đề', 'Bạn đã bắt đầu theo dõi chủ đề nghiên cứu. Bạn sẽ nhận được thông báo về các bài báo mới thuộc chủ đề này.', 0, DATEADD(minute, -8, GETDATE()));
END
GO
