output "file_path" {
  description = "Where the generated file lives"
  value       = local_file.proof.filename
}

output "random_hex" {
  description = "The value Terraform computed and stored in the state"
  value       = random_id.suffix.hex
}
