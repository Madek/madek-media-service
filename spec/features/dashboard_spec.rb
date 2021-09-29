require 'spec_helper'

describe "displaying dashboard", type: :feature do
  let(:user) { create(:user, :with_system_admin_role) }
  let(:username) do
    person = user.person
    "#{person.first_name} #{person.last_name}"
  end

  before do
    sign_in
  end

  it "displays it ;)" do
    visit '/media-service/'

    expect(page).to have_selector('a.dropdown-toggle', text: username)
  end
end
