import { useId, useState } from 'react'

interface PasswordFieldProps {
  label: string
  value: string
  onChange: (value: string) => void
  /** Overrides the generated one where a stable id is needed, e.g. for a test or a label. */
  id?: string
  autoComplete?: string
  required?: boolean
  autoFocus?: boolean
  help?: string
}

/**
 * A password box with a reveal toggle.
 *
 * <p>Typing a password blind is how a typo becomes a failed sign-in that reads like a forgotten
 * password. The toggle only changes the input's type, so the value is never handled separately.
 */
export function PasswordField({
  label,
  value,
  onChange,
  id,
  autoComplete,
  required,
  autoFocus,
  help,
}: PasswordFieldProps) {
  const generatedId = useId()
  const fieldId = id ?? generatedId
  const [visible, setVisible] = useState(false)

  return (
    <div className="field">
      <label htmlFor={fieldId}>{label}</label>

      <div className="password-field">
        <input
          id={fieldId}
          type={visible ? 'text' : 'password'}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          autoComplete={autoComplete}
          required={required}
          autoFocus={autoFocus}
        />

        <button
          type="button"
          className="password-toggle"
          // The button is the control; the state it reports is whether the password is showing.
          aria-pressed={visible}
          aria-label={visible ? 'Hide password' : 'Show password'}
          title={visible ? 'Hide password' : 'Show password'}
          onClick={() => setVisible((shown) => !shown)}
        >
          {visible ? <EyeOffIcon /> : <EyeIcon />}
        </button>
      </div>

      {help && <span className="help">{help}</span>}
    </div>
  )
}

function EyeIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

function EyeOffIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M10.6 6.2A9.9 9.9 0 0 1 12 6c6.5 0 10 7 10 7a15.6 15.6 0 0 1-3 3.9" />
      <path d="M6.2 6.4A15.7 15.7 0 0 0 2 13s3.5 7 10 7a9.7 9.7 0 0 0 4.3-1" />
      <path d="M3 3l18 18" />
    </svg>
  )
}
