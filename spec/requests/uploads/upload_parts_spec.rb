require "requests/uploads/shared/configuration"
require "requests/shared/authorization_error"

describe "Uploads", type: :request do
  include_context "configuration"

  describe "sending file parts" do
    let!(:settings) { create(:media_service_setting, upload_min_part_size: chunk_size) }
    let(:store) { create(:media_store, :database, :with_users, users: [user].compact) }
    let(:chunk_size) { 100 }
    let(:part_1_request) do
      faraday_client_for_upload.put("parts/0", part_1) do |req|
        req.params = {
          start: 0,
          size: chunk_size,
          md5: part_1_md5
        }
      end
    end
    let(:part_2_request) do
      faraday_client_for_upload.put("parts/1", part_2) do |req|
        req.params = {
          start: chunk_size,
          size: 32,
          md5: part_2_md5
        }
      end
    end
    def upload_get_request
      faraday_client_with_token(json_response: true).get("uploads/#{upload_id}")
    end

    shared_examples "successful response" do
      before do
        start_request
      end

      it "uploads file in 2 parts" do
        expect(without_timestamps(part_1_request.body)).to match({
          id: a_kind_of(String),
          md5: part_1_md5,
          media_file_id: nil,
          part: 0,
          sha256: Digest::SHA256.hexdigest(part_1),
          size: chunk_size,
          start: 0,
          upload_id: upload_id,
        }.stringify_keys)

        expect(without_timestamps(part_2_request.body)).to match({
          id: a_kind_of(String),
          md5: part_2_md5,
          media_file_id: nil,
          part: 1,
          sha256: Digest::SHA256.hexdigest(part_2),
          size: 32,
          start: chunk_size,
          upload_id: upload_id,
        }.stringify_keys)
      end

      describe "GET: uploads/:upload_id" do
        context "when all parts of file have been sent" do
          before do
            part_1_request
            part_2_request
            complete_request
          end

          it "responds with upload details with 'completed' state" do
            expect(without_timestamps(upload_get_request.body)).to eq({
              id: upload_id,
              md5: md5,
              state: "completed",
              media_store_id: "database",
              size: file_size,
              uploader_id: user.id,
              content_type: "text/plain",
              filename: "small.txt",
              media_file_id: nil,
              error: nil,
              sha256: nil,
            }.stringify_keys)
          end

          it "2nd call responds with upload details with 'finished' state" do
            upload_get_request
            sleep 1 # ugly!
            expect(without_timestamps(upload_get_request.body)).to match({
              id: upload_id,
              md5: md5,
              state: "finished",
              media_store_id: "database",
              size: file_size,
              uploader_id: user.id,
              content_type: "text/plain",
              filename: "small.txt",
              error: nil,
              sha256: Digest::SHA256.hexdigest(file),
              media_file_id: a_kind_of(String)
            }.stringify_keys)
          end
        end

        context "when only first part of file has been sent" do
          before do
            part_1_request
            complete_request
          end

          it "responds with upload details with 'completed' state" do
            expect(without_timestamps(upload_get_request.body)).to eq({
              id: upload_id,
              md5: md5,
              state: "completed",
              media_store_id: "database",
              size: file_size,
              uploader_id: user.id,
              content_type: "text/plain",
              filename: "small.txt",
              media_file_id: nil,
              error: nil,
              sha256: nil,
            }.stringify_keys)
          end

          it "2nd call responds with upload details with 'failed' state "\
            "but without sha256 and media_file_id values and with error" do
            upload_get_request
            sleep 2 # ugly!
            response_body = upload_get_request.body
            expect(without_timestamps(response_body)).to match({
              id: upload_id,
              md5: md5,
              state: "failed",
              media_store_id: "database",
              size: file_size,
              uploader_id: user.id,
              content_type: "text/plain",
              filename: "small.txt",
              media_file_id: nil,
              sha256: nil,
              error: a_kind_of(String)
            }.stringify_keys)
          end
        end
      end
    end

    context "with public access" do
      let(:user) { nil }
      let(:user_token) { nil }

      it "raises the wrapped error" do
        expect { part_1_request }.to raise_error do |error|
          expect(error).to be_an_instance_of(Faraday::ParsingError)
          expect(error.response.status).to eq(401)
          expect(error.response.body).to include("Authentication/Sign-in required")
        end
      end
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }

      include_examples "successful response"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }

      include_examples "successful response"
    end

    context "for user with system admin role" do
      let(:user) { create(:user, :with_system_admin_role) }

      include_examples "successful response"
    end
  end
end
