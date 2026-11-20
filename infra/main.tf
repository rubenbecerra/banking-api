# One droplet with Docker installed by cloud-init, and a firewall in front.
#
# Why a droplet and not a Kubernetes cluster: the goal is one API, one database
# and one Redis. Kubernetes would add a control plane to operate without adding
# a single guarantee this project needs.

resource "digitalocean_ssh_key" "admin" {
  name       = "${var.project_name}-admin"
  public_key = file(pathexpand(var.ssh_public_key_path))
}

resource "digitalocean_droplet" "api" {
  name     = "${var.project_name}-api"
  image    = "ubuntu-24-04-x64"
  size     = var.droplet_size
  region   = var.region
  ssh_keys = [digitalocean_ssh_key.admin.fingerprint]

  # cloud-init runs once, on first boot: Docker, firewall and swap.
  user_data = file("${path.module}/cloud-init.yaml")

  tags = [var.project_name]
}

resource "digitalocean_firewall" "api" {
  name = "${var.project_name}-fw"

  droplet_ids = [digitalocean_droplet.api.id]

  inbound_rule {
    protocol         = "tcp"
    port_range       = "22"
    source_addresses = [var.ssh_allowed_cidr]
  }

  inbound_rule {
    protocol         = "tcp"
    port_range       = "80"
    source_addresses = ["0.0.0.0/0", "::/0"]
  }

  inbound_rule {
    protocol         = "tcp"
    port_range       = "443"
    source_addresses = ["0.0.0.0/0", "::/0"]
  }

  inbound_rule {
    protocol         = "icmp"
    source_addresses = ["0.0.0.0/0", "::/0"]
  }

  outbound_rule {
    protocol              = "tcp"
    port_range            = "1-65535"
    destination_addresses = ["0.0.0.0/0", "::/0"]
  }

  outbound_rule {
    protocol              = "udp"
    port_range            = "1-65535"
    destination_addresses = ["0.0.0.0/0", "::/0"]
  }
}
