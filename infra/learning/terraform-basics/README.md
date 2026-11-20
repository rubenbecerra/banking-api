# Terraform crash 1 — without a cloud account

The goal is the cycle, not the resource: `init -> plan -> apply -> state -> destroy`.
DigitalOcean's credit lands on 23 Nov; nothing here needs an account.

## Cycle

```powershell
cd infra\learning\terraform-basics

terraform init                 # downloads the providers, creates .terraform/
terraform fmt -check -recursive
terraform validate
terraform plan -out=tfplan     # shows +2 to add, reads nothing yet
terraform apply tfplan         # writes the file and the state
terraform state list           # what Terraform thinks it owns
terraform show                 # every attribute, including the computed ones
terraform plan                 # "No changes": that is idempotency
terraform destroy              # removes the file; the state goes back to empty
```

## What to look at (this is what an interview asks)

1. **State** (`terraform.tfstate`): Terraform's memory of what exists. It is not
   a config file and it must never be committed: it can contain secrets.
2. **Plan vs apply**: `plan` compares the desired state (`.tf`) with the real
   state. `apply` executes that diff.
3. **Computed values**: `random_id.suffix.hex` did not exist until `apply`; it
   was stored in the state and reused afterwards.
4. **Idempotency**: running `apply` twice does nothing the second time. This is
   the same property our APIs have with `Idempotency-Key`.
5. **Destroy**: `terraform destroy` removes what the state says it created,
   nothing else. Deleting the state by hand would orphan resources.

## Drill (5 min, do it without looking)

`terraform destroy -auto-approve`, then `terraform apply -auto-approve`, then
`terraform plan` -> "No changes". Say out loud what the state is for.
