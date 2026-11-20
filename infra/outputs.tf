output "droplet_ip" {
  description = "Public IPv4. Point the DNS record here."
  value       = digitalocean_droplet.api.ipv4_address
}

output "ssh_command" {
  description = "Ready-to-paste login"
  value       = "ssh root@${digitalocean_droplet.api.ipv4_address}"
}
