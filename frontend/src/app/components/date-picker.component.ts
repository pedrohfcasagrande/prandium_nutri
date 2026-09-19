import { Component, Input, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule, ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule, MatDatepickerInputEvent } from '@angular/material/datepicker';
import { MatIconModule } from '@angular/material/icon';

/**
 * Date picker que expõe [(ngModel)] como string ISO "YYYY-MM-DD".
 * Internamente usa FormControl<Date> conectado ao MatDatepicker.
 */
@Component({
  selector: 'app-date-picker',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatDatepickerModule, MatIconModule
  ],
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => DatePickerComponent), multi: true }
  ],
  template: `
    <mat-form-field appearance="outline" subscriptSizing="dynamic" class="full">
      <mat-label>{{ label }}</mat-label>
      <input matInput [matDatepicker]="picker" [formControl]="ctrl"
             (dateChange)="onDateChange($event)" (blur)="onTouched()">
      <mat-datepicker-toggle matIconSuffix [for]="picker"></mat-datepicker-toggle>
      <mat-datepicker #picker></mat-datepicker>
    </mat-form-field>
  `,
  styles: [`.full { width: 100%; }`]
})
export class DatePickerComponent implements ControlValueAccessor {
  @Input() label = '';
  ctrl = new FormControl<Date | null>(null);

  private onChange: (v: string | null) => void = () => {};
  onTouched: () => void = () => {};

  writeValue(iso: string | null): void {
    this.ctrl.setValue(iso ? this.isoToDate(iso) : null, { emitEvent: false });
  }
  registerOnChange(fn: any) { this.onChange = fn; }
  registerOnTouched(fn: any) { this.onTouched = fn; }
  setDisabledState(d: boolean) { d ? this.ctrl.disable() : this.ctrl.enable(); }

  onDateChange(ev: MatDatepickerInputEvent<Date>) {
    const d = ev.value;
    this.onChange(d ? this.dateToIso(d) : null);
  }

  private dateToIso(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  private isoToDate(iso: string): Date {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d);
  }
}
