variable "environment" {
  description = "Name written into the generated file"
  type        = string
  default     = "learning"

  validation {
    condition     = contains(["learning", "staging", "prod"], var.environment)
    error_message = "environment must be one of: learning, staging, prod"
  }
}

# A real resource with a computed value: Terraform generates it and stores it in
# the state. "Computed" is why "plan" can show changes before "apply".
resource "random_id" "suffix" {
  byte_length = 4
}

# A resource that touches the file system. It is idempotent: if the file already
# exists with the same content, the next apply does nothing.
resource "local_file" "proof" {
  filename = "${path.module}/generated/${var.environment}-${random_id.suffix.hex}.txt"

  content = <<-EOT
    environment = ${var.environment}
    random      = ${random_id.suffix.hex}
    applied_by  = terraform
  EOT
}
