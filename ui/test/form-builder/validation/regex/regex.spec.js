import {Regex} from 'form-builder/validation/regex/regex';
import util from '../validator-test-util';

describe('Regex validator', function() {

  var validatorDef = {
    id: 'regex',
    context: {
      pattern: null
    }
  };
  var validationModelNoValue = {
    'testfield': {
      value: null
    }
  };
  var validationModelWithValue = {
    'testfield': {
      value: 'val1'
    }
  };
  var flatViewModel = {
    'testfield': {}
  };

  it('should throw error if no validation pattern is provided', () => {
    var validator = new Regex();
    expect(() => { validator.validate('testfield', validatorDef, validationModelWithValue) }).to.throw(Error);
  });

  it('should throw error if invalid regex pattern is found for a field', () => {
    var validator = new Regex();
    validatorDef.context.pattern = '((.{1,4}';
    expect(() => { validator.validate('testfield', validatorDef, validationModelWithValue) }).to.throw(Error);
  });

  it('should put every new pattern inside the regex cache', () => {
    var validator = new Regex();
    validatorDef.context.pattern = '[\\s\\S]{1,4}';
    validator.validate('testfield', validatorDef, validationModelWithValue);
    validatorDef.context.pattern = '.{1,4}';
    validator.validate('testfield', validatorDef, validationModelWithValue);
    expect(validator.getRegexCache()).to.have.all.keys('[\\s\\S]{1,4}', '.{1,4}');
  });

  it('should not duplicate patterns in the regex cache', () => {
    var validator = new Regex();
    validatorDef.context.pattern = '[\\s\\S]{1,4}';
    validator.validate('testfield', validatorDef, validationModelWithValue);
    validatorDef.context.pattern = '.{1,4}';
    validator.validate('testfield', validatorDef, validationModelWithValue);
    validatorDef.context.pattern = '[\\s\\S]{1,4}';
    validator.validate('testfield', validatorDef, validationModelWithValue);
    expect(validator.getRegexCache()).to.have.all.keys('[\\s\\S]{1,4}', '.{1,4}');
  });

  it('should mark fields with invalid data as visible', () => {
    var validator = new Regex();
    validatorDef.context.pattern = '[\\s\\S]{1,4}';
    var validationModelWithValue = {
      'testfield': {
        value: 'invalid value'
      }
    };
    validator.validate('testfield', validatorDef, validationModelWithValue, flatViewModel);
    expect(flatViewModel.testfield.rendered).to.be.true;
  });

  it('test some patterns', function() {
    var validator = new Regex();
    testdata.forEach((set) => {
      validatorDef.context.pattern = set.pattern;
      set.data.forEach((data) => {
        validationModelNoValue.testfield.value = data.value;
        var isValid = validator.validate('testfield', validatorDef, validationModelNoValue, flatViewModel);
        var result = isValid === data.valid;
        assert.isTrue(result, 'Value "' + validationModelNoValue.testfield.value + '" should be [' + (data.valid ? 'valid' : 'invalid') + '] according to regex ' + validatorDef.context.pattern);
      });
    });
  });

  var testdata = [
    {
      pattern: '[\\s\\S]{1,4}',
      data: [
        { value: 'a', valid: true },
        { value: ' a', valid: true },
        { value: 'a ', valid: true },
        { value: ' a ', valid: true },
        { value: 'abcd', valid: true },
        { value: 'a bc', valid: true },
        { value: '1234', valid: true },
        { value: 1234, valid: true },
        { value: '12ab', valid: true },
        { value: ' 12ab ', valid: true },// value is trimmed before evaluation
        { value: 'abcde', valid: false },
        { value: '', valid: true },
        { value: null, valid: true },
        { value: undefined, valid: true },
        { value: ' ', valid: true },
        { value: '    ', valid: true }
      ]
    },
    {
      pattern: '.{1,4}',
      data: [
        { value: 'a', valid: true },
        { value: 'ab', valid: true },
        { value: 'abc', valid: true },
        { value: 'abcd', valid: true },
        { value: 'abcde', valid: false },
        { value: '', valid: true },
        { value: ' ', valid: true }
      ]
    }
  ];
});