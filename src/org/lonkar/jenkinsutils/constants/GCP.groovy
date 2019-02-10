package org.lonkar.jenkinsutils.constants;
import java.io.Serializable;

/**
 * Google cloud platform constants for using in jenkins pipeline.
 *
 */
class GCP implements Serializable {
  static def Regions = [
      "us-west1",
      "asia-east1",
      "asia-east2",
      "asia-northeast1",
      "asia-south1",
      "asia-southeast1",
      "australia-southeast1",
      "europe-north1",
      "europe-west1",
      "europe-west2",
      "europe-west3",
      "europe-west4",
      "northamerica-northeast1",
      "southamerica-east1",
      "us-central1",
      "us-east1",
      "us-east4",
      "us-west1",
      "us-west2"
  ];

  static def Zones = [
    "us-east1-b",
    "us-east1-c",
    "us-east1-d",
    "us-east4-c",
    "us-east4-b",
    "us-east4-a",
    "us-central1-c",
    "us-central1-a",
    "us-central1-f",
    "us-central1-b",
    "us-west1-b",
    "us-west1-c",
    "us-west1-a",
    "europe-west4-a",
    "europe-west4-b",
    "europe-west4-c",
    "europe-west1-b",
    "europe-west1-d",
    "europe-west1-c",
    "europe-west3-c",
    "europe-west3-a",
    "europe-west3-b",
    "europe-west2-c",
    "europe-west2-b",
    "europe-west2-a",
    "asia-east1-b",
    "asia-east1-a",
    "asia-east1-c",
    "asia-southeast1-b",
    "asia-southeast1-a",
    "asia-southeast1-c",
    "asia-northeast1-b",
    "asia-northeast1-c",
    "asia-northeast1-a",
    "asia-south1-c",
    "asia-south1-b",
    "asia-south1-a",
    "australia-southeast1-b",
    "australia-southeast1-c",
    "australia-southeast1-a",
    "southamerica-east1-b",
    "southamerica-east1-c",
    "southamerica-east1-a",
    "asia-east2-a",
    "asia-east2-b",
    "asia-east2-c",
    "europe-north1-a",
    "europe-north1-b",
    "europe-north1-c",
    "northamerica-northeast1-a",
    "northamerica-northeast1-b",
    "northamerica-northeast1-c",
    "us-west2-a",
    "us-west2-b",
    "us-west2-c"
  ];

  static def MachineTypes = [
      "n1-standard-2",
      "f1-micro",
      "g1-small",
      "n1-standard-1",
      "n1-standard-2",
      "n1-standard-4",
      "n1-standard-8",
      "n1-standard-16",
      "n1-standard-32",
      "n1-highmem-2",
      "n1-highmem-4",
      "n1-highmem-8",
      "n1-highmem-16",
      "n1-highmem-32",
      "n1-highcpu-2",
      "n1-highcpu-4",
      "n1-highcpu-8",
      "n1-highcpu-16",
      "n1-highcpu-32"
  ];

  static def K8sMasterVersions = [
    "1.11.6-gke.6",
    "1.11.6-gke.3",
    "1.11.6-gke.2",
    "1.11.6-gke.0",
    "1.11.5-gke.5",
    "1.10.12-gke.1",
    "1.10.12-gke.0",
    "1.10.11-gke.1"
  ];
}
