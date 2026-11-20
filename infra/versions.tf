terraform {
  required_version = ">= 1.9"

  required_providers {
    digitalocean = {
      source  = "digitalocean/digitalocean"
      version = "~> 2.49"
    }
  }
}

# The token comes from the DIGITALOCEAN_TOKEN environment variable. Never in a
# file, never in git. Create it at https://cloud.digitalocean.com/account/api/tokens
provider "digitalocean" {}
