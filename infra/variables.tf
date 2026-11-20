variable "project_name" {
  description = "Prefix for every resource name"
  type        = string
  default     = "banking"
}

variable "region" {
  description = "DigitalOcean region (fra1 = Frankfurt, good latency from Spain)"
  type        = string
  default     = "fra1"
}

variable "droplet_size" {
  description = "s-1vcpu-2gb is ~12 USD/month and leaves room for Postgres, Redis and the API"
  type        = string
  default     = "s-1vcpu-2gb"
}

variable "ssh_public_key_path" {
  description = "Path to the public key that will be able to log in as root"
  type        = string
  default     = "~/.ssh/id_ed25519.pub"
}

variable "ssh_allowed_cidr" {
  description = "CIDR allowed to reach port 22. Tighten it to your home IP before the demo"
  type        = string
  default     = "0.0.0.0/0"
}
