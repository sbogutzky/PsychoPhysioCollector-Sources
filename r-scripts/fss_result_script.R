setwd("~/Entwicklung/projects/bogutzky/repositories/data-collector-android/r-scripts")
source("/Users/simonbogutzky/Entwicklung/projects/bogutzky/repositories/flow-gait-analysis/r-code/scripts/final/functions/fss-functions.R")

directory.name <- "2014-05-19_17-12-43"

scale <- read.csv(paste("../data/", directory.name, "/scale.csv", sep =""))
results <- CalculateFlowShortScaleFactors(cbind(scale[3:18], scale$System.Timestamp.01, scale$System.Timestamp.02))
summary(results)

